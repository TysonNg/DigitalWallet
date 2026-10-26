package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.*;
import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import com.tyson.digitalwallet.ddd.domain.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReconcileWithdrawalUseCaseImpl implements ReconcileWithdrawalUseCase {
    private static final Logger log = LoggerFactory.getLogger(ReconcileWithdrawalUseCaseImpl.class);
    private final WithdrawalRepository withdrawals;
    private final PayoutNotificationInbox inbox;
    private final ProcessWithdrawalUseCase processor;
    private final WithdrawalProcessingPolicy policy;

    public ReconcileWithdrawalUseCaseImpl(
            WithdrawalRepository withdrawals,
            PayoutNotificationInbox inbox,
            ProcessWithdrawalUseCase processor,
            WithdrawalProcessingPolicy policy) {
        this.withdrawals = withdrawals;
        this.inbox = inbox;
        this.processor = processor;
        this.policy = policy;
    }

    @Override
    public void reconcile() {
        for (var notification : inbox.pending()) {
            try {
                processor.process(notification.withdrawalId());
                inbox.complete(notification.provider(), notification.eventId());
            } catch (RuntimeException ex) {
                log.warn("Payout notification {} awaits retry", notification.eventId());
            }
        }
        for (var id :
                policy.usesKafka() ? withdrawals.findRecoveryIds() : withdrawals.findPendingIds()) {
            try {
                processor.process(id);
            } catch (RuntimeException ex) {
                log.warn("Withdrawal {} awaits reconciliation", id);
            }
        }
    }
}
