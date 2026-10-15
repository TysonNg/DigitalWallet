package com.tyson.digitalwallet.ddd.application.usecase.payment.impl;

import com.tyson.digitalwallet.ddd.application.usecase.payment.command.ProcessPaymentWebhookCommand;
import com.tyson.digitalwallet.ddd.application.usecase.payment.resolver.PaymentGatewayResolver;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import com.tyson.digitalwallet.ddd.domain.repository.*;
import com.tyson.digitalwallet.ddd.domain.service.payment.PaymentService;
import com.tyson.digitalwallet.ddd.domain.service.payment.port.PaymentGatewayPort;
import com.tyson.digitalwallet.ddd.domain.service.payment.result.PaymentWebhookResult;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.DepositResult;
import com.tyson.digitalwallet.ddd.domain.exception.DistributedLockException;
import com.tyson.digitalwallet.ddd.domain.service.lock.DistributedLockService;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.Before;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class PaymentWebhookValidationTest {
    private final PaymentGatewayPort gateway = mock(PaymentGatewayPort.class);
    private final PaymentOrderRepository orders = mock(PaymentOrderRepository.class);
    private final WalletRepository walletRepository = mock(WalletRepository.class);
    private final WalletService wallets = mock(WalletService.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final DistributedLockService lockService = mock(DistributedLockService.class);
    private final org.springframework.transaction.PlatformTransactionManager txManager = mock(org.springframework.transaction.PlatformTransactionManager.class);
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate = new org.springframework.transaction.support.TransactionTemplate(txManager) {
        @Override
        public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
            return action.doInTransaction(null);
        }
    };
    private final PaymentOrder order = PaymentOrder.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("500000.00"), PaymentGatewayType.STRIPE);
    private final Wallet wallet = new Wallet(order.getWalletId(), order.getUserId(), BigDecimal.ZERO, "VND", WalletStatus.ACTIVE, null, null);
    private ProcessPaymentWebhookUseCaseImpl useCase;
    @Before public void setup() {
        when(gateway.getGatewayType()).thenReturn(PaymentGatewayType.STRIPE);
        when(orders.findByGatewayOrderIdWithLock("cs_test")).thenReturn(Optional.of(order));
        when(walletRepository.findById(order.getWalletId())).thenReturn(Optional.of(wallet));
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(4);
            task.run();
            return null;
        }).when(lockService).executeWithLock(anyString(), anyLong(), anyLong(), any(), any(Runnable.class));
        useCase = new ProcessPaymentWebhookUseCaseImpl(
                new PaymentGatewayResolver(List.of(gateway)),
                orders,
                payments,
                wallets,
                walletRepository,
                events,
                lockService,
                transactionTemplate
        );
    }
    private void process(PaymentWebhookResult result) {
        when(gateway.verifyWebhook(anyString(), anyMap())).thenReturn(result);
        useCase.processWebhook(new ProcessPaymentWebhookCommand(PaymentGatewayType.STRIPE, "{}", Map.of()));
    }
    private void rejected(PaymentWebhookResult result) {
        process(result);
        verifyNoInteractions(wallets, payments, events);
        assertTrue(order.isPending());
    }
    @Test public void mismatchingAmountDoesNotCreditOrCompleteOrder() {
        rejected(PaymentWebhookResult.success("cs_test", new BigDecimal("499999"), "vnd"));
    }
    @Test public void greaterAmountDoesNotCreditOrder() {
        rejected(PaymentWebhookResult.success("cs_test", new BigDecimal("500001"), "vnd"));
    }
    @Test public void missingAmountDoesNotCreditOrder() {
        rejected(PaymentWebhookResult.success("cs_test", null, "vnd"));
    }
    @Test public void wrongCurrencyDoesNotCreditOrder() {
        rejected(PaymentWebhookResult.success("cs_test", new BigDecimal("500000"), "usd"));
    }
    @Test public void missingCurrencyDoesNotCreditOrder() {
        rejected(PaymentWebhookResult.success("cs_test", new BigDecimal("500000"), null));
    }
    @Test public void walletInAnotherCurrencyDoesNotReceiveVnd() {
        wallet.setCurrency("USD");
        rejected(PaymentWebhookResult.success("cs_test", new BigDecimal("500000"), "vnd"));
    }
    @Test public void wrongGatewayDoesNotCreditOrder() {
        order.setGateway(PaymentGatewayType.PAYOS);
        rejected(PaymentWebhookResult.success("cs_test", new BigDecimal("500000"), "vnd"));
    }
    @Test public void unpaidEventKeepsOrderPending() {
        rejected(PaymentWebhookResult.pending("cs_test", new BigDecimal("500000"), "vnd"));
    }
    @Test public void invalidSignatureHasNoSideEffects() {
        rejected(PaymentWebhookResult.invalid("Invalid signature"));
        verifyNoInteractions(orders);
    }
    @Test public void unpaidThenPaidThenDuplicateCreditsOnceDespiteScaleAndCaseDifferences() {
        BigDecimal amount = new BigDecimal("500000.00");
        var depositTransaction = Transaction.createDeposit(wallet.getId(), amount, BigDecimal.ZERO, amount);
        when(wallets.deposit(wallet.getId(), amount)).thenReturn(new DepositResult(wallet, depositTransaction));
        doAnswer(invocation -> { order.markAsSuccess(invocation.getArgument(1)); return order; })
                .when(payments).completeOrder(eq(order.getId()), any());
        rejected(PaymentWebhookResult.pending("cs_test", new BigDecimal("500000"), "vnd"));
        var result = PaymentWebhookResult.success("cs_test", new BigDecimal("500000"), "vnd");
        process(result);
        process(result);
        verify(wallets, times(1)).deposit(wallet.getId(), amount);
        verify(payments, times(1)).completeOrder(order.getId(), depositTransaction.getId());
        verify(events, times(1)).publishEvent(any(Object.class));
        assertTrue(order.isSuccess());
    }

    @Test
    public void whenWebhookProcessed_acquiresDistributedLockWithGatewayOrderId() {
        BigDecimal amount = new BigDecimal("500000.00");
        var depositTransaction = Transaction.createDeposit(wallet.getId(), amount, BigDecimal.ZERO, amount);
        when(wallets.deposit(wallet.getId(), amount)).thenReturn(new DepositResult(wallet, depositTransaction));
        doAnswer(invocation -> { order.markAsSuccess(invocation.getArgument(1)); return order; })
                .when(payments).completeOrder(eq(order.getId()), any());

        var result = PaymentWebhookResult.success("cs_test", new BigDecimal("500000"), "vnd");
        process(result);

        verify(lockService, times(1)).executeWithLock(
                eq("lock:payment:webhook:cs_test"),
                eq(3L),
                eq(10L),
                eq(TimeUnit.SECONDS),
                any(Runnable.class)
        );
    }

    @Test
    public void whenLockUnavailable_throwsDistributedLockExceptionAndDoesNotTouchDatabase() {
        doThrow(new DistributedLockException("Lock is busy"))
                .when(lockService)
                .executeWithLock(eq("lock:payment:webhook:cs_test"), anyLong(), anyLong(), any(), any(Runnable.class));

        var result = PaymentWebhookResult.success("cs_test", new BigDecimal("500000"), "vnd");
        when(gateway.verifyWebhook(anyString(), anyMap())).thenReturn(result);

        try {
            useCase.processWebhook(new ProcessPaymentWebhookCommand(PaymentGatewayType.STRIPE, "{}", Map.of()));
            fail("Expected DistributedLockException");
        } catch (DistributedLockException e) {
            assertEquals("Lock is busy", e.getMessage());
        }

        // Đảm bảo không đụng đến DB nếu lock không chiếm được
        verifyNoInteractions(orders);
        verifyNoInteractions(wallets);
        verifyNoInteractions(payments);
    }
}