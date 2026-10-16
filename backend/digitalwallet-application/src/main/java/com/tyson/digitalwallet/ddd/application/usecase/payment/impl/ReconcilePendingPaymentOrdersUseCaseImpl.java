package com.tyson.digitalwallet.ddd.application.usecase.payment.impl;

import com.tyson.digitalwallet.ddd.application.port.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.application.port.payment.PaymentGatewayPort;
import com.tyson.digitalwallet.ddd.application.usecase.payment.ReconcilePendingPaymentOrdersUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.payment.resolver.PaymentGatewayResolver;
import com.tyson.digitalwallet.ddd.domain.event.MoneyDepositEvent;
import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;
import com.tyson.digitalwallet.ddd.domain.repository.PaymentOrderRepository;
import com.tyson.digitalwallet.ddd.domain.service.payment.PaymentService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.DepositResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ReconcilePendingPaymentOrdersUseCaseImpl implements ReconcilePendingPaymentOrdersUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReconcilePendingPaymentOrdersUseCaseImpl.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentGatewayResolver gatewayResolver;
    private final PaymentService paymentService;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;
    private final DistributedLockService lockService;
    private final TransactionTemplate transactionTemplate;

    @Value("${payment.order.expiration-minutes:15}")
    private int expirationMinutes;

    public ReconcilePendingPaymentOrdersUseCaseImpl(
            PaymentOrderRepository paymentOrderRepository,
            PaymentGatewayResolver gatewayResolver,
            PaymentService paymentService,
            WalletService walletService,
            ApplicationEventPublisher eventPublisher,
            DistributedLockService lockService,
            TransactionTemplate transactionTemplate
    ) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.gatewayResolver = gatewayResolver;
        this.paymentService = paymentService;
        this.walletService = walletService;
        this.eventPublisher = eventPublisher;
        this.lockService = lockService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void reconcile() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(expirationMinutes);
        List<PaymentOrder> pendingOrders = paymentOrderRepository.findPendingOrdersCreatedBefore(threshold);

        if (pendingOrders.isEmpty()) {
            return;
        }

        log.info("Found {} pending payment orders older than {} minutes to reconcile", pendingOrders.size(), expirationMinutes);

        for (PaymentOrder order : pendingOrders) {
            try {
                reconcileSingleOrder(order);
            } catch (Exception e) {
                log.error("Failed to reconcile payment order {}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }

    private void reconcileSingleOrder(PaymentOrder order) {
        String lockKey = (order.getGatewayOrderId() != null && !order.getGatewayOrderId().isBlank())
                ? "lock:payment:webhook:" + order.getGatewayOrderId()
                : "lock:payment:order:" + order.getId();

        lockService.executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, () -> {
            transactionTemplate.execute(status -> {
                doReconcileOrderInternal(order);
                return null;
            });
        });
    }

    private void doReconcileOrderInternal(PaymentOrder candidate) {
        Optional<PaymentOrder> freshOrderOpt = paymentOrderRepository.findByIdWithLock(candidate.getId());
        if (freshOrderOpt.isEmpty()) {
            log.warn("Payment order {} not found during reconciliation lock", candidate.getId());
            return;
        }

        PaymentOrder order = freshOrderOpt.get();
        if (!order.isPending()) {
            log.info("Payment order {} is no longer PENDING (status={}), skipping reconciliation.", order.getId(), order.getStatus());
            return;
        }

        if (order.getGatewayOrderId() == null || order.getGatewayOrderId().isBlank()) {
            log.info("Payment order {} has no gatewayOrderId and timed out. Marking as EXPIRED.", order.getId());
            paymentService.expireOrder(order.getId());
            return;
        }

        PaymentGatewayPort gateway = gatewayResolver.resolve(order.getGateway());
        PaymentOrderStatus gatewayStatus = gateway.queryOrderStatus(order.getGatewayOrderId());

        switch (gatewayStatus) {
            case SUCCESS -> {
                log.warn("RECONCILIATION: Dropped webhook detected for order {}. Gateway confirmed SUCCESS, depositing {} to wallet {}",
                        order.getId(), order.getAmount(), order.getWalletId());
                DepositResult depositResult = walletService.deposit(order.getWalletId(), order.getAmount());
                paymentService.completeOrder(order.getId(), depositResult.transaction().getId());
                eventPublisher.publishEvent(new MoneyDepositEvent(
                        depositResult.transaction().getId(),
                        depositResult.wallet().getUserId(),
                        order.getAmount(),
                        depositResult.wallet().getBalance(),
                        Instant.now()
                ));
                log.info("Successfully fulfilled order {} during reconciliation. Transaction ID: {}",
                        order.getId(), depositResult.transaction().getId());
            }
            case FAILED -> {
                log.info("Payment order {} marked as FAILED by gateway during reconciliation.", order.getId());
                paymentService.failOrder(order.getId());
            }
            case CANCELLED -> {
                log.info("Payment order {} marked as CANCELLED by gateway during reconciliation.", order.getId());
                paymentService.cancelOrder(order.getId());
            }
            case EXPIRED, PENDING -> {
                log.info("Payment order {} not paid and exceeded {} minutes. Marking as EXPIRED.", order.getId(), expirationMinutes);
                paymentService.expireOrder(order.getId());
            }
        }
    }

    public void setExpirationMinutes(int expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
}
