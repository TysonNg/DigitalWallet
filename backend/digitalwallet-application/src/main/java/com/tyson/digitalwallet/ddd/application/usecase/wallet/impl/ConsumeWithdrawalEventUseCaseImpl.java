package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.InvalidWithdrawalEventException;
import com.tyson.digitalwallet.ddd.application.port.inbox.*;
import com.tyson.digitalwallet.ddd.application.port.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus;
import com.tyson.digitalwallet.ddd.domain.repository.WithdrawalRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ConsumeWithdrawalEventUseCaseImpl implements ConsumeWithdrawalEventUseCase {
    public static final String CONSUMER = "withdrawal-process-v1";
    private final InboxEventStore inbox;
    private final WithdrawalRepository withdrawals;
    private final ProcessWithdrawalUseCase processor;
    private final DistributedLockService locks;
    private final TransactionTemplate tx;

    public ConsumeWithdrawalEventUseCaseImpl(
            InboxEventStore inbox,
            WithdrawalRepository withdrawals,
            ProcessWithdrawalUseCase processor,
            DistributedLockService locks,
            TransactionTemplate tx) {
        this.inbox = inbox;
        this.withdrawals = withdrawals;
        this.processor = processor;
        this.locks = locks;
        this.tx = tx;
    }

    @Override
    public void consume(UUID eventId, UUID withdrawalId) {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(withdrawalId);
        locks.executeWithLock(
                "lock:inbox:" + CONSUMER + ":" + eventId,
                0,
                -1,
                TimeUnit.SECONDS,
                () -> {
                    var receipt = tx.execute(s -> inbox.find(CONSUMER, eventId));
                    if (receipt.isPresent()) {
                        if (!receipt.get().aggregateId().equals(withdrawalId.toString()))
                            throw new InvalidWithdrawalEventException(
                                    "Event ID was already used for another withdrawal");
                        return null;
                    }
                    var status =
                            tx.execute(
                                    s ->
                                            withdrawals
                                                    .findByIdWithLock(withdrawalId)
                                                    .orElseThrow(
                                                            () ->
                                                                    new IllegalStateException(
                                                                            "Withdrawal not"
                                                                                + " available yet"))
                                                    .getStatus());
                    if (status == WithdrawalStatus.PENDING || status == WithdrawalStatus.PROCESSING)
                        processor.process(withdrawalId);
                    // No surrounding transaction spans the provider call. On receipt failure,
                    // replay uses
                    // persisted withdrawal state, checkpoints, the withdrawal lock and stable
                    // provider keys.
                    tx.execute(
                            s -> {
                                inbox.save(
                                        new InboxEventReceipt(
                                                CONSUMER,
                                                eventId,
                                                withdrawalId.toString(),
                                                Instant.now()));
                                return null;
                            });
                    return null;
                });
    }
}
