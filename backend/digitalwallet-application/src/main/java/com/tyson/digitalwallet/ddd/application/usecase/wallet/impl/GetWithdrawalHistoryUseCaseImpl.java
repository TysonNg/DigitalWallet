package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.*;
import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WithdrawalResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import com.tyson.digitalwallet.ddd.domain.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

@Service
public class GetWithdrawalHistoryUseCaseImpl implements GetWithdrawalHistoryUseCase {
    private final WalletRepository wallets;
    private final WithdrawalRepository withdrawals;
    private final PayoutAttemptRepository attempts;
    private final TransactionTemplate tx;

    public GetWithdrawalHistoryUseCaseImpl(
            WalletRepository wallets,
            WithdrawalRepository withdrawals,
            PayoutAttemptRepository attempts,
            TransactionTemplate tx) {
        this.wallets = wallets;
        this.withdrawals = withdrawals;
        this.attempts = attempts;
        this.tx = tx;
    }

    @Override
    public List<WithdrawalResponse> history(UUID userId, UUID walletId) {
        return tx.execute(
                status -> {
                    var wallet =
                            wallets.findById(walletId)
                                    .orElseThrow(
                                            () -> new IllegalArgumentException("Wallet not found"));
                    if (!wallet.getUserId().equals(userId))
                        throw new SecurityException("Wallet belongs to another user");
                    return withdrawals.findByWallet(walletId).stream()
                            .map(r -> WithdrawalResponse.from(r, attempt(r.getId())))
                            .toList();
                });
    }

    private PayoutAttempt attempt(UUID id) {
        return attempts.findByWithdrawalId(id)
                .orElseThrow(() -> new IllegalStateException("Payout attempt missing"));
    }
}
