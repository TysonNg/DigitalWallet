package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.*;
import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import com.tyson.digitalwallet.ddd.domain.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

@Service
public class HandlePayoutNotificationUseCaseImpl implements HandlePayoutNotificationUseCase {
    private final WithdrawalRepository withdrawals;
    private final PayoutAttemptRepository attempts;
    private final TransactionTemplate tx;
    private final WithdrawalWorkQueue workQueue;
    private final PayoutGatewayRegistry gateways;
    private final PayoutNotificationInbox inbox;

    public HandlePayoutNotificationUseCaseImpl(
            WithdrawalRepository withdrawals,
            PayoutAttemptRepository attempts,
            TransactionTemplate tx,
            PayoutGatewayRegistry gateways,
            PayoutNotificationInbox inbox, WithdrawalWorkQueue workQueue) {
        this.withdrawals = withdrawals;
        this.attempts = attempts;
        this.tx = tx;
        this.workQueue = workQueue;
        this.gateways = gateways;
        this.inbox = inbox;
    }

    @Override
    public void webhook(String provider, String body, String signature) {
        var notice = gateways.get(provider).verifyWebhook(body, signature);
        if (notice == null) return;
        tx.execute(
                s -> {
                    var request = withdrawals.findByIdWithLock(notice.withdrawalId());
                    if (request.isEmpty()) return null;
                    var a = attempt(notice.withdrawalId());
                    if (!provider.equals(a.getProvider())
                            || a.getAmount() != notice.amount()
                            || !a.getCurrency().equalsIgnoreCase(notice.currency())
                            || !Objects.equals(
                                    a.getMetadata().get("account_reference"),
                                    notice.accountReference())
                            || (a.getMetadata().get("payout_reference") != null
                                    && !Objects.equals(
                                            a.getMetadata().get("payout_reference"),
                                            notice.payoutReference())))
                        throw new IllegalArgumentException("Webhook does not match withdrawal");
                    inbox.enqueue(provider, notice.eventId(), notice.withdrawalId());
                    return null;
                });
    }

    private PayoutAttempt attempt(UUID id) {
        return attempts.findByWithdrawalId(id)
                .orElseThrow(() -> new IllegalStateException("Payout attempt missing"));
    }
}
