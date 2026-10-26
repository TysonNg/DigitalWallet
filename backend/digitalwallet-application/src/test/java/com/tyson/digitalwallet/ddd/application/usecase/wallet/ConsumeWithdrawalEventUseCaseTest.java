package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tyson.digitalwallet.ddd.application.exception.InvalidWithdrawalEventException;
import com.tyson.digitalwallet.ddd.application.port.inbox.*;
import com.tyson.digitalwallet.ddd.application.port.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.impl.ConsumeWithdrawalEventUseCaseImpl;
import com.tyson.digitalwallet.ddd.domain.model.entity.WithdrawalRequest;
import com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus;
import com.tyson.digitalwallet.ddd.domain.repository.WithdrawalRepository;

import org.junit.*;
import org.springframework.transaction.support.*;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public class ConsumeWithdrawalEventUseCaseTest {
    private final UUID eventId = UUID.randomUUID(), withdrawalId = UUID.randomUUID();
    private final Map<UUID, InboxEventReceipt> receipts = new HashMap<>();
    private final InboxEventStore inbox = mock(InboxEventStore.class);
    private final WithdrawalRepository withdrawals = mock(WithdrawalRepository.class);
    private final ProcessWithdrawalUseCase processor = mock(ProcessWithdrawalUseCase.class);
    private final WithdrawalRequest request = mock(WithdrawalRequest.class);
    private ConsumeWithdrawalEventUseCase consumer;

    @Before
    public void setup() {
        var tx =
                new TransactionTemplate() {
                    @Override
                    public <T> T execute(TransactionCallback<T> c) {
                        return c.doInTransaction(null);
                    }
                };
        var locks = mock(DistributedLockService.class);
        when(locks.executeWithLock(
                        anyString(),
                        anyLong(),
                        anyLong(),
                        any(),
                        org.mockito.ArgumentMatchers.<Supplier<Object>>any()))
                .thenAnswer(i -> ((Supplier<?>) i.getArgument(4)).get());
        when(inbox.find(anyString(), any()))
                .thenAnswer(i -> Optional.ofNullable(receipts.get(i.getArgument(1))));
        doAnswer(
                        i -> {
                            InboxEventReceipt r = i.getArgument(0);
                            receipts.put(r.eventId(), r);
                            return null;
                        })
                .when(inbox)
                .save(any());
        when(withdrawals.findByIdWithLock(withdrawalId)).thenReturn(Optional.of(request));
        when(request.getStatus()).thenReturn(WithdrawalStatus.PENDING);
        consumer = new ConsumeWithdrawalEventUseCaseImpl(inbox, withdrawals, processor, locks, tx);
    }

    @Test
    public void duplicateEventDoesNotProcessAgain() {
        consumer.consume(eventId, withdrawalId);
        consumer.consume(eventId, withdrawalId);
        verify(processor, times(1)).process(withdrawalId);
        assertEquals(withdrawalId.toString(), receipts.get(eventId).aggregateId());
    }

    @Test
    public void processingFailureHasNoCompletedReceiptAndCanRetry() {
        doThrow(new IllegalStateException()).doNothing().when(processor).process(withdrawalId);
        assertThrows(IllegalStateException.class, () -> consumer.consume(eventId, withdrawalId));
        assertTrue(receipts.isEmpty());
        consumer.consume(eventId, withdrawalId);
        verify(processor, times(2)).process(withdrawalId);
    }

    @Test
    public void terminalBacklogDoesNotCallProviderButCompletesInbox() {
        for (var state :
                List.of(
                        WithdrawalStatus.SUCCEEDED,
                        WithdrawalStatus.FAILED,
                        WithdrawalStatus.RETURNED,
                        WithdrawalStatus.REVIEW_REQUIRED)) {
            when(request.getStatus()).thenReturn(state);
            consumer.consume(UUID.randomUUID(), withdrawalId);
        }
        verifyNoInteractions(processor);
        assertEquals(4, receipts.size());
    }

    @Test
    public void reusedEventIdWithDifferentWithdrawalIsRejected() {
        receipts.put(
                eventId,
                new InboxEventReceipt(
                        "withdrawal-process-v1",
                        eventId,
                        UUID.randomUUID().toString(),
                        Instant.now()));
        assertThrows(
                InvalidWithdrawalEventException.class,
                () -> consumer.consume(eventId, withdrawalId));
        verifyNoInteractions(processor);
    }

    @Test
    public void missingWithdrawalIsRetryableAndNotRecorded() {
        when(withdrawals.findByIdWithLock(withdrawalId)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> consumer.consume(eventId, withdrawalId));
        assertTrue(receipts.isEmpty());
        verifyNoInteractions(processor);
    }

    @Test
    public void receiptFailureIsPropagatedSoKafkaDoesNotAck() {
        doThrow(new IllegalStateException("database unavailable")).when(inbox).save(any());
        assertThrows(IllegalStateException.class, () -> consumer.consume(eventId, withdrawalId));
        verify(processor).process(withdrawalId);
    }
}
