package com.tyson.digitalwallet.ddd.application.usecase.payment.impl;

import com.tyson.digitalwallet.ddd.application.port.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.application.port.payment.PaymentGatewayPort;
import com.tyson.digitalwallet.ddd.application.usecase.payment.resolver.PaymentGatewayResolver;
import com.tyson.digitalwallet.ddd.domain.event.MoneyDepositEvent;
import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;
import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionType;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.PaymentOrderRepository;
import com.tyson.digitalwallet.ddd.domain.service.payment.PaymentService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.DepositResult;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ReconcilePendingPaymentOrdersUseCaseImplTest {

    private final PaymentGatewayPort gateway = mock(PaymentGatewayPort.class);
    private final PaymentOrderRepository orders = mock(PaymentOrderRepository.class);
    private final WalletService wallets = mock(WalletService.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final DistributedLockService lockService = mock(DistributedLockService.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
    private final TransactionTemplate transactionTemplate = new TransactionTemplate(txManager) {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(null);
        }
    };

    private ReconcilePendingPaymentOrdersUseCaseImpl useCase;

    @Before
    public void setup() {
        when(gateway.getGatewayType()).thenReturn(PaymentGatewayType.PAYOS);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(4);
            task.run();
            return null;
        }).when(lockService).executeWithLock(anyString(), anyLong(), anyLong(), any(), any(Runnable.class));

        useCase = new ReconcilePendingPaymentOrdersUseCaseImpl(
                orders,
                new PaymentGatewayResolver(List.of(gateway)),
                payments,
                wallets,
                events,
                lockService,
                transactionTemplate
        );
        useCase.setExpirationMinutes(15);
    }

    @Test
    public void testReconcile_RecoversDroppedWebhookWhenGatewayReportsSuccess() {
        PaymentOrder order = PaymentOrder.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100000"), PaymentGatewayType.PAYOS);
        order.updateCheckoutInfo("123456", "https://checkout.payos.vn/123456");

        when(orders.findPendingOrdersCreatedBefore(any(LocalDateTime.class))).thenReturn(List.of(order));
        when(orders.findByIdWithLock(order.getId())).thenReturn(Optional.of(order));
        when(gateway.queryOrderStatus("123456")).thenReturn(PaymentOrderStatus.SUCCESS);

        Wallet wallet = new Wallet(order.getWalletId(), order.getUserId(), new BigDecimal("100000"), "VND", WalletStatus.ACTIVE, null, null);
        Transaction tx = mock(Transaction.class);
        UUID txId = UUID.randomUUID();
        when(tx.getId()).thenReturn(txId);
        when(wallets.deposit(order.getWalletId(), order.getAmount())).thenReturn(new DepositResult(wallet, tx));

        useCase.reconcile();

        verify(wallets).deposit(order.getWalletId(), order.getAmount());
        verify(payments).completeOrder(order.getId(), txId);
        verify(events).publishEvent(any(MoneyDepositEvent.class));
        verify(payments, never()).expireOrder(any());
    }

    @Test
    public void testReconcile_ExpiresOrderWhenGatewayReportsPendingOrExpired() {
        PaymentOrder order = PaymentOrder.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100000"), PaymentGatewayType.PAYOS);
        order.updateCheckoutInfo("123456", "https://checkout.payos.vn/123456");

        when(orders.findPendingOrdersCreatedBefore(any(LocalDateTime.class))).thenReturn(List.of(order));
        when(orders.findByIdWithLock(order.getId())).thenReturn(Optional.of(order));
        when(gateway.queryOrderStatus("123456")).thenReturn(PaymentOrderStatus.PENDING);

        useCase.reconcile();

        verify(payments).expireOrder(order.getId());
        verify(wallets, never()).deposit(any(), any());
    }

    @Test
    public void testReconcile_ExpiresOrderWhenNoGatewayOrderId() {
        PaymentOrder order = PaymentOrder.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100000"), PaymentGatewayType.PAYOS);

        when(orders.findPendingOrdersCreatedBefore(any(LocalDateTime.class))).thenReturn(List.of(order));
        when(orders.findByIdWithLock(order.getId())).thenReturn(Optional.of(order));

        useCase.reconcile();

        verify(payments).expireOrder(order.getId());
        verify(gateway, never()).queryOrderStatus(anyString());
        verify(wallets, never()).deposit(any(), any());
    }

    @Test
    public void testReconcile_SkipsWhenOrderNoLongerPending() {
        PaymentOrder order = PaymentOrder.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100000"), PaymentGatewayType.PAYOS);
        order.updateCheckoutInfo("123456", "https://checkout.payos.vn/123456");
        order.markAsSuccess(UUID.randomUUID());

        when(orders.findPendingOrdersCreatedBefore(any(LocalDateTime.class))).thenReturn(List.of(order));
        when(orders.findByIdWithLock(order.getId())).thenReturn(Optional.of(order));

        useCase.reconcile();

        verify(gateway, never()).queryOrderStatus(anyString());
        verify(wallets, never()).deposit(any(), any());
        verify(payments, never()).expireOrder(any());
    }

    @Test
    public void testReconcile_CancelsOrderWhenGatewayReportsCancelled() {
        PaymentOrder order = PaymentOrder.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100000"), PaymentGatewayType.PAYOS);
        order.updateCheckoutInfo("123456", "https://checkout.payos.vn/123456");

        when(orders.findPendingOrdersCreatedBefore(any(LocalDateTime.class))).thenReturn(List.of(order));
        when(orders.findByIdWithLock(order.getId())).thenReturn(Optional.of(order));
        when(gateway.queryOrderStatus("123456")).thenReturn(PaymentOrderStatus.CANCELLED);

        useCase.reconcile();

        verify(payments).cancelOrder(order.getId());
        verify(wallets, never()).deposit(any(), any());
    }

    @Test
    public void testReconcile_FailsOrderWhenGatewayReportsFailed() {
        PaymentOrder order = PaymentOrder.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100000"), PaymentGatewayType.PAYOS);
        order.updateCheckoutInfo("123456", "https://checkout.payos.vn/123456");

        when(orders.findPendingOrdersCreatedBefore(any(LocalDateTime.class))).thenReturn(List.of(order));
        when(orders.findByIdWithLock(order.getId())).thenReturn(Optional.of(order));
        when(gateway.queryOrderStatus("123456")).thenReturn(PaymentOrderStatus.FAILED);

        useCase.reconcile();

        verify(payments).failOrder(order.getId());
        verify(wallets, never()).deposit(any(), any());
    }
}
