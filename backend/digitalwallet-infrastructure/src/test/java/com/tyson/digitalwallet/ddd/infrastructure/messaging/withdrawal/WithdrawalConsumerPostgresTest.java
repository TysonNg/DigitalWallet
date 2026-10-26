package com.tyson.digitalwallet.ddd.infrastructure.messaging.withdrawal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tyson.digitalwallet.ddd.application.port.inbox.*;
import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.impl.*;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;

import org.junit.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class WithdrawalConsumerPostgresTest {
    private WithdrawalConsumerFixture f;

    @Before
    public void setup() throws Exception {
        var url = System.getProperty("outbox.test.jdbc");
        Assume.assumeTrue("Isolated PostgreSQL required", url != null);
        f = new WithdrawalConsumerFixture(url);
    }

    @After
    public void cleanup() throws Exception {
        if (f != null) f.close();
    }

    @Test
    public void sharedReceiptIdentityIsConsumerScopedAndRollbackIsAtomic() {
        var event = UUID.randomUUID();
        f.tx.execute(
                s -> {
                    f.inbox.save(new InboxEventReceipt("a", event, "w1", Instant.now()));
                    f.inbox.save(new InboxEventReceipt("b", event, "w1", Instant.now()));
                    return null;
                });
        assertEquals(2, f.receipts());
        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        f.tx.execute(
                                s -> {
                                    f.inbox.save(
                                            new InboxEventReceipt("a", event, "w2", Instant.now()));
                                    return null;
                                }));
        assertEquals("w1", f.tx.execute(s -> f.inbox.find("a", event).orElseThrow().aggregateId()));
        assertThrows(
                IllegalStateException.class,
                () ->
                        f.tx.execute(
                                s -> {
                                    f.inbox.save(
                                            new InboxEventReceipt("c", event, "w1", Instant.now()));
                                    throw new IllegalStateException();
                                }));
        assertEquals(2, f.receipts());
    }

    @Test
    public void restartAfterProviderSuccessBeforeReceiptDoesNotCreateAnotherPayout() {
        var id = f.create();
        var event = UUID.randomUUID();
        f.outcome.set(PayoutGateway.Outcome.PAID);
        f.jdbc.execute(
                "ALTER TABLE inbox_event ADD CONSTRAINT reject_receipts CHECK (consumer_name <>"
                    + " 'withdrawal-process-v1')");
        assertThrows(DataIntegrityViolationException.class, () -> f.consumer.consume(event, id));
        assertEquals(WithdrawalStatus.SUCCEEDED, f.status(id));
        assertEquals(0, f.receipts());
        f.jdbc.execute("ALTER TABLE inbox_event DROP CONSTRAINT reject_receipts");
        f.consumer.consume(event, id);
        f.consumer.consume(event, id);
        assertEquals(1, f.payoutCreations.get());
        assertEquals(1, f.gatewayCalls.get());
        assertEquals(1, f.receipts());
        verify(f.ledger, times(1)).recordWithdraw(any(), eq(f.walletId), any(), any(), anyString());
    }

    @Test
    public void recoverySelectionExcludesFreshRequestsAndIncludesInterruptedAndLegacyStarts() {
        var fresh = f.create();
        assertFalse(f.tx.execute(s -> f.withdrawals.findRecoveryIds()).contains(fresh));
        f.failuresLeft.set(1);
        assertThrows(IllegalStateException.class, () -> f.processor.process(fresh));
        assertEquals(WithdrawalStatus.PROCESSING, f.status(fresh));
        assertTrue(f.tx.execute(s -> f.withdrawals.findRecoveryIds()).contains(fresh));
        // Simulate a pre-migration request whose provider start was checkpointed while status
        // stayed PENDING.
        f.jdbc.update("UPDATE withdrawal_requests SET status='PENDING' WHERE id=?", fresh);
        assertTrue(f.tx.execute(s -> f.withdrawals.findRecoveryIds()).contains(fresh));
        f.jdbc.update(
                "UPDATE payout_attempts SET status='PENDING',provider_metadata=CAST(? AS jsonb)"
                    + " WHERE withdrawal_id=?",
                "{\"account_started\":\"true\"}",
                fresh);
        assertTrue(f.tx.execute(s -> f.withdrawals.findRecoveryIds()).contains(fresh));
        f.jdbc.update(
                "UPDATE payout_attempts SET provider_metadata='{}'::jsonb WHERE withdrawal_id=?",
                fresh);
        assertFalse(f.tx.execute(s -> f.withdrawals.findRecoveryIds()).contains(fresh));
    }

    @Test
    public void providerTimeoutRetainsHoldAndReconciliationCompletesWithoutKafkaReplay() {
        var id = f.create();
        f.failuresLeft.set(1);
        assertThrows(IllegalStateException.class, () -> f.consumer.consume(UUID.randomUUID(), id));
        assertEquals(0, f.receipts());
        assertEquals(WithdrawalStatus.PROCESSING, f.status(id));
        assertEquals(
                0,
                new BigDecimal("800000")
                        .compareTo(
                                f.jdbc.queryForObject(
                                        "SELECT balance FROM wallets WHERE id=?",
                                        BigDecimal.class,
                                        f.walletId)));
        f.outcome.set(PayoutGateway.Outcome.PAID);
        new ReconcileWithdrawalUseCaseImpl(
                        f.withdrawals,
                        mock(PayoutNotificationInbox.class),
                        f.processor,
                        new WithdrawalProcessingPolicy("kafka"))
                .reconcile();
        assertEquals(WithdrawalStatus.SUCCEEDED, f.status(id));
        assertEquals(1, f.payoutCreations.get());
    }
}
