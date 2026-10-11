package com.tyson.digitalwallet.ddd.domain.service.payment.impl;

import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.repository.PaymentOrderRepository;
import com.tyson.digitalwallet.ddd.domain.service.payment.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;

    public PaymentServiceImpl(PaymentOrderRepository paymentOrderRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
    }

    @Override
    public PaymentOrder initializeOrder(UUID walletId, UUID userId, BigDecimal amount, PaymentGatewayType gateway) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        PaymentOrder order = PaymentOrder.create(walletId, userId, amount, gateway);
        return paymentOrderRepository.save(order);
    }

    @Override
    public PaymentOrder updateCheckoutInfo(UUID orderId, String gatewayOrderId, String checkoutUrl) {
        PaymentOrder order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + orderId));
        order.updateCheckoutInfo(gatewayOrderId, checkoutUrl);
        return paymentOrderRepository.save(order);
    }

    @Override
    public PaymentOrder completeOrder(UUID orderId, UUID transactionId) {
        PaymentOrder order = paymentOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + orderId));
        order.markAsSuccess(transactionId);
        return paymentOrderRepository.save(order);
    }

    @Override
    public PaymentOrder failOrder(UUID orderId) {
        PaymentOrder order = paymentOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + orderId));
        order.markAsFailed();
        return paymentOrderRepository.save(order);
    }

    @Override
    public PaymentOrder cancelOrder(UUID orderId) {
        PaymentOrder order = paymentOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + orderId));
        order.markAsCancelled();
        return paymentOrderRepository.save(order);
    }

    @Override
    public PaymentOrder expireOrder(UUID orderId) {
        PaymentOrder order = paymentOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + orderId));
        order.markAsExpired();
        return paymentOrderRepository.save(order);
    }
}
