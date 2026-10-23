package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.*;
import com.tyson.digitalwallet.ddd.application.port.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.domain.event.MoneyWithdrawnEvent;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import com.tyson.digitalwallet.ddd.domain.repository.*;
import com.tyson.digitalwallet.ddd.domain.service.ledger.LedgerService;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ProcessWithdrawalUseCaseImpl implements ProcessWithdrawalUseCase {
    private final WalletRepository wallets;
    private final WithdrawalRepository withdrawals;
    private final PayoutAttemptRepository attempts;
    private final TransactionRepository transactions;
    private final LedgerService ledger;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;
    private final PayoutGatewayRegistry gateways;
    private final DistributedLockService locks;

    public ProcessWithdrawalUseCaseImpl(
            WalletRepository wallets,
            WithdrawalRepository withdrawals,
            PayoutAttemptRepository attempts,
            TransactionRepository transactions,
            LedgerService ledger,
            TransactionTemplate tx,
            ApplicationEventPublisher events,
            PayoutGatewayRegistry gateways,
            DistributedLockService locks) {
        this.wallets = wallets;
        this.withdrawals = withdrawals;
        this.attempts = attempts;
        this.transactions = transactions;
        this.ledger = ledger;
        this.tx = tx;
        this.events = events;
        this.gateways = gateways;
        this.locks = locks;
    }

    @Override
    public void process(UUID id) {
        locks.executeWithLock(
                "lock:withdrawal:" + id,
                0,
                -1,
                TimeUnit.SECONDS,
                () -> {
                    processLocked(id);
                    return null;
                });
    }

    private void processLocked(UUID id) {
        var r = tx.execute(s -> required(id));
        if (r.getStatus() == WithdrawalStatus.FAILED
                || r.getStatus() == WithdrawalStatus.RETURNED
                || r.getStatus() == WithdrawalStatus.REVIEW_REQUIRED) return;
        var a = tx.execute(s -> attempt(id));
        var gateway = gateways.get(a.getProvider());
        try {
            var result =
                    "true".equals(a.getMetadata().get("compensation_pending"))
                            ? new PayoutGateway.Result(
                                    PayoutGateway.Outcome.FAILED,
                                    a.getMetadata()
                                            .getOrDefault(
                                                    "failure_reason", "Provider payout failed"))
                            : gateway.execute(
                                    r, a, checkpoint -> tx.execute(s -> attempts.save(checkpoint)));
            if (result.outcome() == PayoutGateway.Outcome.FAILED) {
                // Persist the recovery phase before reversing provider funds. A restart must not
                // submit payout again.
                var current =
                        tx.execute(
                                s -> {
                                    required(id);
                                    return attempts.save(
                                            attempt(id)
                                                    .checkpoint("compensation_pending", "true")
                                                    .checkpoint(
                                                            "failure_reason",
                                                            result.detail() == null
                                                                    ? "Provider payout failed"
                                                                    : result.detail()));
                                });
                gateway.compensate(
                        r,
                        current); // Must complete provider compensation before releasing the hold.
            }
            tx.execute(
                    s -> {
                        var current = required(id);
                        switch (result.outcome()) {
                            case PAID -> settle(current);
                            case FAILED -> release(current, result.detail());
                            case REVIEW_REQUIRED -> {
                                if (current.isHeld()) current.requireReview(result.detail());
                                else current.recordError(result.detail());
                            }
                            case PENDING -> {
                                if (current.getStatus() == WithdrawalStatus.PENDING)
                                    current.markProcessing();
                                current.recordError(null);
                            }
                        }
                        // Touch successful requests too, so periodic reconciliation is fair across
                        // a batch.
                        if (current.getStatus() == WithdrawalStatus.SUCCEEDED)
                            current.recordError(current.getError());
                        withdrawals.save(current);
                        String attemptStatus =
                                switch (result.outcome()) {
                                    case PAID -> "SUCCEEDED";
                                    case FAILED ->
                                            current.getStatus() == WithdrawalStatus.RETURNED
                                                    ? "RETURNED"
                                                    : "FAILED";
                                    case REVIEW_REQUIRED -> "REVIEW_REQUIRED";
                                    case PENDING -> "PROCESSING";
                                };
                        attempts.save(
                                attempt(id)
                                        .checkpoint("compensation_pending", "false")
                                        .withStatus(
                                                PayoutAttemptStatus.valueOf(attemptStatus),
                                                result.detail()));
                        return null;
                    });
        } catch (RuntimeException ex) {
            tx.execute(
                    s -> {
                        var current = required(id);
                        current.recordError("Provider processing requires retry or reconciliation");
                        withdrawals.save(current);
                        attempts.save(
                                attempt(id)
                                        .withStatus(
                                                PayoutAttemptStatus.UNKNOWN, current.getError()));
                        return null;
                    });
            throw ex;
        }
    }

    private void settle(WithdrawalRequest r) {
        if (r.getStatus() == WithdrawalStatus.SUCCEEDED) return;
        if (!r.isHeld()) return;
        var wallet = wallets.findByIdWithLock(r.getWalletId()).orElseThrow();
        var t =
                Transaction.createWithdraw(
                        r.getWalletId(),
                        r.getAmount(),
                        r.getBalanceBefore(),
                        r.getBalanceBefore().subtract(r.getAmount()));
        t.setDescription("Withdrawal " + r.getId());
        transactions.save(t);
        ledger.recordWithdraw(
                t.getId(), r.getWalletId(), r.getAmount(), BigDecimal.ZERO, t.getDescription());
        r.complete(t.getId());
        events.publishEvent(
                new MoneyWithdrawnEvent(
                        t.getId(),
                        r.getUserId(),
                        r.getAmount(),
                        wallet.getBalance(),
                        Instant.now()));
    }

    private void release(WithdrawalRequest r, String reason) {
        if (r.getStatus() == WithdrawalStatus.FAILED || r.getStatus() == WithdrawalStatus.RETURNED)
            return;
        var wallet = wallets.findByIdWithLock(r.getWalletId()).orElseThrow();
        wallet.releaseWithdrawalHold(r.getAmount());
        wallets.saveWallet(wallet);
        if (r.getStatus() == WithdrawalStatus.SUCCEEDED) {
            ledger.recordWithdrawReturn(
                    r.getTransactionId(),
                    r.getWalletId(),
                    r.getAmount(),
                    "Returned withdrawal " + r.getId());
            var t = transactions.findById(r.getTransactionId()).orElseThrow();
            t.setStatus(TransactionStatus.FAILED);
            transactions.save(t);
            r.markReturned(reason);
        } else r.fail(reason);
    }

    private WithdrawalRequest required(UUID id) {
        return withdrawals
                .findByIdWithLock(id)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found"));
    }

    private PayoutAttempt attempt(UUID id) {
        return attempts.findByWithdrawalId(id)
                .orElseThrow(() -> new IllegalStateException("Payout attempt missing"));
    }
}
