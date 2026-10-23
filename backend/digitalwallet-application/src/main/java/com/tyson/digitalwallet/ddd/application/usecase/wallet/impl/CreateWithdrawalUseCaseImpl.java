package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.*;
import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.application.port.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWithdrawalCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.pin.WalletPinAttemptService;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WithdrawalResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import com.tyson.digitalwallet.ddd.domain.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

@Service
public class CreateWithdrawalUseCaseImpl implements CreateWithdrawalUseCase {
    private final WalletRepository wallets;
    private final WithdrawalRepository withdrawals;
    private final PayoutAttemptRepository attempts;
    private final PasswordHasher hasher;
    private final WalletPinAttemptService pins;
    private final TransactionTemplate tx;
    private final WithdrawalWorkQueue workQueue;
    private final PayoutGatewayRegistry gateways;

    public CreateWithdrawalUseCaseImpl(
            WalletRepository wallets,
            WithdrawalRepository withdrawals,
            PayoutAttemptRepository attempts,
            PasswordHasher hasher,
            WalletPinAttemptService pins,
            TransactionTemplate tx,
            PayoutGatewayRegistry gateways, WithdrawalWorkQueue workQueue) {
        this.wallets = wallets;
        this.withdrawals = withdrawals;
        this.attempts = attempts;
        this.hasher = hasher;
        this.pins = pins;
        this.tx = tx;
        this.workQueue = workQueue;
        this.gateways = gateways;
    }

    @Override
    public WithdrawalResponse create(CreateWithdrawalCommand c) {
        if (c == null
                || c.walletId() == null
                || c.userId() == null
                || c.idempotencyKey() == null
                || !c.idempotencyKey().matches("[A-Za-z0-9_-]{8,100}"))
            throw new IllegalArgumentException("Invalid withdrawal request");
        return tx.execute(
                status -> {
                    var wallet =
                            wallets.findByIdWithLock(c.walletId())
                                    .orElseThrow(
                                            () -> new IllegalArgumentException("Wallet not found"));
                    if (!wallet.getUserId().equals(c.userId()))
                        throw new SecurityException("Wallet belongs to another user");
                    var previous = withdrawals.findByWalletAndKey(c.walletId(), c.idempotencyKey());
                    if (previous.isPresent()) {
                        var r = previous.get();
                        var a = attempt(r.getId());
                        if (c.amount() == null
                                || r.getAmount().compareTo(c.amount()) != 0
                                || !r.getDestination().equals(c.destination())
                                || !a.getProvider().equals(c.provider()))
                            throw new IllegalArgumentException(
                                    "Idempotency key already used with different details");
                        return WithdrawalResponse.from(r, a);
                    }
                    var quote =
                            gateways.get(c.provider())
                                    .validate(c.amount(), wallet.getCurrency(), c.destination());
                    if (wallet.hasPin()) {
                        pins.assertNotLocked(wallet.getId());
                        if (c.pin() == null || !hasher.matches(c.pin(), wallet.getPinHash())) {
                            if (c.pin() != null) pins.recordFailedAttempt(wallet.getId());
                            throw new AppException(
                                    ErrorCode.INVALID_WALLET_PIN, "Invalid or missing wallet PIN");
                        }
                        pins.resetAttempts(wallet.getId());
                    }
                    var r =
                            WithdrawalRequest.create(
                                    wallet.getId(),
                                    c.userId(),
                                    c.amount(),
                                    wallet.getBalance(),
                                    wallet.getCurrency(),
                                    c.destination(),
                                    c.idempotencyKey());
                    wallet.withdraw(c.amount());
                    wallets.saveWallet(wallet);
                    withdrawals.save(r);
                    var a =
                            attempts.save(
                                    PayoutAttempt.create(
                                                    r.getId(),
                                                    c.provider(),
                                                    quote.amount(),
                                                    quote.currency())
                                            .checkpoint(
                                                    "currency_exponent",
                                                    Integer.toString(quote.currencyExponent())));
                    return WithdrawalResponse.from(r, a);
                });
    }

    private PayoutAttempt attempt(UUID id) {
        return attempts.findByWithdrawalId(id)
                .orElseThrow(() -> new IllegalStateException("Payout attempt missing"));
    }
}
