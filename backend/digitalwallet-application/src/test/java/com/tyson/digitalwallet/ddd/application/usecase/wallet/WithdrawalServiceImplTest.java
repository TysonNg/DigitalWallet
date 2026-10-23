package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tyson.digitalwallet.ddd.application.port.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.application.port.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWithdrawalCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.impl.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.pin.WalletPinAttemptService;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import com.tyson.digitalwallet.ddd.domain.repository.*;
import com.tyson.digitalwallet.ddd.domain.service.ledger.LedgerService;

import org.junit.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;

/** Exercises common orchestration independently of any provider SDK. */
public class WithdrawalServiceImplTest {
    private final UUID walletId = UUID.randomUUID(), userId = UUID.randomUUID();
    private final WalletRepository wallets = mock(WalletRepository.class);
    private final WithdrawalRepository requests = mock(WithdrawalRepository.class);
    private final PayoutAttemptRepository attempts = mock(PayoutAttemptRepository.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final LedgerService ledger = mock(LedgerService.class);
    private final PayoutGateway gateway = mock(PayoutGateway.class);
    private final PayoutNotificationInbox inbox = mock(PayoutNotificationInbox.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final WalletPinAttemptService pins = mock(WalletPinAttemptService.class);
    private final Map<UUID, WithdrawalRequest> stored = new HashMap<>();
    private final Map<UUID, PayoutAttempt> executions = new HashMap<>();
    private Wallet wallet;
    private Services service;

    @Before
    public void setup() {
        wallet =
                new Wallet(
                        walletId,
                        userId,
                        new BigDecimal("1000000"),
                        "VND",
                        WalletStatus.ACTIVE,
                        null,
                        null);
        when(wallets.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
        when(wallets.findById(walletId)).thenReturn(Optional.of(wallet));
        when(requests.findByWalletAndKey(eq(walletId), anyString()))
                .thenAnswer(
                        i ->
                                stored.values().stream()
                                        .filter(r -> r.getIdempotencyKey().equals(i.getArgument(1)))
                                        .findFirst());
        when(requests.findByIdWithLock(any()))
                .thenAnswer(i -> Optional.ofNullable(stored.get(i.getArgument(0))));
        when(requests.save(any()))
                .thenAnswer(
                        i -> {
                            WithdrawalRequest r = i.getArgument(0);
                            stored.put(r.getId(), r);
                            return r;
                        });
        when(attempts.findByWithdrawalId(any()))
                .thenAnswer(i -> Optional.ofNullable(executions.get(i.getArgument(0))));
        when(attempts.save(any()))
                .thenAnswer(
                        i -> {
                            PayoutAttempt a = i.getArgument(0);
                            executions.put(a.getWithdrawalId(), a);
                            return a;
                        });
        when(gateway.provider()).thenReturn("EXAMPLE");
        when(gateway.validate(any(), anyString(), anyString()))
                .thenReturn(new PayoutGateway.Quote(800, "USD", 2));
        when(requests.existsHeldWithdrawalByWalletId(any()))
                .thenAnswer(
                        i ->
                                stored.values().stream()
                                        .anyMatch(
                                                r ->
                                                        r.getWalletId().equals(i.getArgument(0))
                                                                && r.isHeld()));
        outcome(PayoutGateway.Outcome.PENDING);
        TransactionTemplate tx =
                new TransactionTemplate() {
                    @Override
                    public <T> T execute(TransactionCallback<T> c) {
                        return c.doInTransaction(null);
                    }
                };
        DistributedLockService locks = mock(DistributedLockService.class);
        when(locks.executeWithLock(
                        anyString(),
                        anyLong(),
                        anyLong(),
                        any(),
                        org.mockito.ArgumentMatchers.<Supplier<Object>>any()))
                .thenAnswer(i -> ((Supplier<?>) i.getArgument(4)).get());
        var registry = new PayoutGatewayRegistry(List.of(gateway));
        var events = mock(ApplicationEventPublisher.class);
        service =
                new Services(
                        new CreateWithdrawalUseCaseImpl(
                                wallets, requests, attempts, hasher, pins, tx, registry),
                        new GetWithdrawalHistoryUseCaseImpl(wallets, requests, attempts, tx),
                        new ProcessWithdrawalUseCaseImpl(
                                wallets,
                                requests,
                                attempts,
                                transactions,
                                ledger,
                                tx,
                                events,
                                registry,
                                locks),
                        new HandlePayoutNotificationUseCaseImpl(
                                requests, attempts, tx, registry, inbox));
    }

    private CreateWithdrawalCommand command(String provider, BigDecimal amount) {
        return new CreateWithdrawalCommand(
                userId, walletId, amount, "request-key-123", provider, "recipient-token", null);
    }

    private UUID create() {
        return service.create(command("EXAMPLE", new BigDecimal("200000"))).id();
    }

    private void outcome(PayoutGateway.Outcome o) {
        when(gateway.execute(any(), any(), any()))
                .thenReturn(new PayoutGateway.Result(o, "provider detail"));
    }

    private void balance(String expected) {
        assertEquals(0, new BigDecimal(expected).compareTo(wallet.getBalance()));
    }

    @Test
    public void replayHoldsOnlyOnceAndKeepsAttemptIdentity() {
        var id = create();
        var attempt = executions.get(id);
        assertEquals(id, create());
        assertSame(attempt, executions.get(id));
        balance("800000");
        verifyNoInteractions(ledger, transactions);
        verify(gateway, never()).execute(any(), any(), any());
    }

    @Test
    public void changedProviderOrAmountCannotReuseRequestKey() {
        create();
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(command("OTHER", new BigDecimal("200000"))));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(command("EXAMPLE", new BigDecimal("300000"))));
        balance("800000");
    }

    @Test
    public void foreignOwnerCannotCreateOrRead() {
        assertThrows(
                SecurityException.class,
                () ->
                        service.create(
                                new CreateWithdrawalCommand(
                                        UUID.randomUUID(),
                                        walletId,
                                        new BigDecimal("200000"),
                                        "request-key-123",
                                        "EXAMPLE",
                                        "recipient-token",
                                        null)));
        assertThrows(SecurityException.class, () -> service.history(UUID.randomUUID(), walletId));
        balance("1000000");
    }

    @Test
    public void pendingDoesNotWriteLedger() {
        var id = create();
        service.process(id);
        assertEquals(WithdrawalStatus.PROCESSING, stored.get(id).getStatus());
        balance("800000");
        verifyNoInteractions(ledger, transactions);
    }

    @Test
    public void paidWritesLedgerExactlyOnce() {
        var id = create();
        outcome(PayoutGateway.Outcome.PAID);
        service.process(id);
        service.process(id);
        balance("800000");
        assertEquals(WithdrawalStatus.SUCCEEDED, stored.get(id).getStatus());
        verify(ledger, times(1))
                .recordWithdraw(
                        any(),
                        eq(walletId),
                        eq(new BigDecimal("200000")),
                        eq(BigDecimal.ZERO),
                        anyString());
    }

    @Test
    public void definitiveFailureCompensatesBeforeRefundAndOnlyOnce() {
        var id = create();
        outcome(PayoutGateway.Outcome.FAILED);
        service.process(id);
        service.process(id);
        balance("1000000");
        assertEquals(WithdrawalStatus.FAILED, stored.get(id).getStatus());
        verify(gateway, times(1)).compensate(any(), any());
        verifyNoInteractions(ledger, transactions);
    }

    @Test
    public void unknownOutcomeKeepsFundsHeld() {
        var id = create();
        when(gateway.execute(any(), any(), any())).thenThrow(new IllegalStateException("timeout"));
        assertThrows(IllegalStateException.class, () -> service.process(id));
        balance("800000");
        assertTrue(stored.get(id).isHeld());
        verify(gateway, never()).compensate(any(), any());
    }

    @Test
    public void failedCompensationCannotReleaseFunds() {
        var id = create();
        outcome(PayoutGateway.Outcome.FAILED);
        doThrow(new IllegalStateException("timeout")).when(gateway).compensate(any(), any());
        assertThrows(IllegalStateException.class, () -> service.process(id));
        balance("800000");
        assertTrue(stored.get(id).isHeld());
    }

    @Test
    public void reviewKeepsHoldAndStopsAutomaticExecution() {
        var id = create();
        outcome(PayoutGateway.Outcome.REVIEW_REQUIRED);
        service.process(id);
        service.process(id);
        balance("800000");
        assertEquals(WithdrawalStatus.REVIEW_REQUIRED, stored.get(id).getStatus());
        verify(gateway, times(1)).execute(any(), any(), any());
    }

    @Test
    public void lateReturnRefundsAndReversesLedgerOnlyOnce() {
        var id = create();
        outcome(PayoutGateway.Outcome.PAID);
        service.process(id);
        var r = stored.get(id);
        when(transactions.findById(r.getTransactionId()))
                .thenReturn(
                        Optional.of(
                                Transaction.createWithdraw(
                                        walletId,
                                        r.getAmount(),
                                        r.getBalanceBefore(),
                                        r.getBalanceBefore().subtract(r.getAmount()))));
        outcome(PayoutGateway.Outcome.FAILED);
        service.process(id);
        service.process(id);
        balance("1000000");
        assertEquals(WithdrawalStatus.RETURNED, r.getStatus());
        verify(ledger, times(1))
                .recordWithdrawReturn(
                        eq(r.getTransactionId()), eq(walletId), eq(r.getAmount()), anyString());
    }

    @Test
    public void missingPinCannotReserveFunds() {
        wallet.setupPin("hash");
        assertThrows(RuntimeException.class, this::create);
        balance("1000000");
        assertTrue(stored.isEmpty());
    }

    @Test
    public void webhookEnqueuesVerifiedWorkWithoutCallingProvider() {
        var id = create();
        when(gateway.verifyWebhook("body", "sig"))
                .thenReturn(new PayoutGateway.Notice("evt1", id, null, null, 800, "USD"));
        service.webhook("EXAMPLE", "body", "sig");
        verify(inbox).enqueue("EXAMPLE", "evt1", id);
        verify(gateway, never()).execute(any(), any(), any());
    }

    @Test
    public void mismatchedWebhookCannotEnqueue() {
        var id = create();
        when(gateway.verifyWebhook("body", "sig"))
                .thenReturn(
                        new PayoutGateway.Notice("evt1", id, "wrong-account", null, 800, "USD"));
        assertThrows(
                IllegalArgumentException.class, () -> service.webhook("EXAMPLE", "body", "sig"));
        verifyNoInteractions(inbox);
    }

    @Test
    public void compensationRetryDoesNotResubmitPayout() {
        var id = create();
        outcome(PayoutGateway.Outcome.FAILED);
        doThrow(new IllegalStateException("reversal timeout"))
                .doNothing()
                .when(gateway)
                .compensate(any(), any());
        assertThrows(IllegalStateException.class, () -> service.process(id));
        service.process(id);
        balance("1000000");
        verify(gateway, times(1)).execute(any(), any(), any());
        verify(gateway, times(2)).compensate(any(), any());
        assertEquals(WithdrawalStatus.FAILED, stored.get(id).getStatus());
    }

    private void assertSecondRequestBlocked() {
        var error =
                assertThrows(
                        com.tyson.digitalwallet.ddd.application.exception.AppException.class,
                        () ->
                                service.create(
                                        new CreateWithdrawalCommand(
                                                userId,
                                                walletId,
                                                new BigDecimal("200000"),
                                                "second-request-key",
                                                "EXAMPLE",
                                                "recipient-token",
                                                null)));
        assertEquals("WITHDRAWAL_ALREADY_IN_PROGRESS", error.getErrorCode().getCode());
        balance("800000");
        assertEquals(1, stored.size());
        assertEquals(1, executions.size());
    }

    @Test
    public void pendingWithdrawalBlocksDifferentKeyButAllowsReplay() {
        var id = create();
        assertEquals(id, create());
        assertSecondRequestBlocked();
    }

    @Test
    public void processingWithdrawalBlocksDifferentKey() {
        var id = create();
        service.process(id);
        assertSecondRequestBlocked();
    }

    @Test
    public void reviewRequiredWithdrawalBlocksDifferentKey() {
        var id = create();
        stored.get(id).requireReview("manual review");
        assertSecondRequestBlocked();
    }

    @Test
    public void completedWithdrawalAllowsNewRequest() {
        create();
        outcome(PayoutGateway.Outcome.PAID);
        service.process(stored.keySet().iterator().next());
        service.create(
                new CreateWithdrawalCommand(
                        userId,
                        walletId,
                        new BigDecimal("200000"),
                        "second-request-key",
                        "EXAMPLE",
                        "recipient-token",
                        null));
        balance("600000");
        assertEquals(2, stored.size());
    }

    @Test
    public void failedWithdrawalAllowsNewRequest() {
        var id = create();
        outcome(PayoutGateway.Outcome.FAILED);
        service.process(id);
        service.create(
                new CreateWithdrawalCommand(
                        userId,
                        walletId,
                        new BigDecimal("200000"),
                        "second-request-key",
                        "EXAMPLE",
                        "recipient-token",
                        null));
        balance("800000");
        assertEquals(2, stored.size());
    }

    @Test
    public void returnedWithdrawalAllowsNewRequest() {
        var id = create();
        var previous = stored.get(id);
        previous.complete(UUID.randomUUID());
        previous.markReturned("returned");
        wallet.releaseWithdrawalHold(previous.getAmount());
        service.create(
                new CreateWithdrawalCommand(
                        userId,
                        walletId,
                        new BigDecimal("200000"),
                        "second-request-key",
                        "EXAMPLE",
                        "recipient-token",
                        null));
        balance("800000");
        assertEquals(2, stored.size());
    }

    private record Services(
            CreateWithdrawalUseCase createUseCase,
            GetWithdrawalHistoryUseCase historyUseCase,
            ProcessWithdrawalUseCase processor,
            HandlePayoutNotificationUseCase notifications) {
        com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WithdrawalResponse create(
                CreateWithdrawalCommand command) {
            return createUseCase.create(command);
        }

        void process(UUID id) {
            processor.process(id);
        }

        Object history(UUID user, UUID wallet) {
            return historyUseCase.history(user, wallet);
        }

        void webhook(String provider, String body, String signature) {
            notifications.webhook(provider, body, signature);
        }
    }
}
