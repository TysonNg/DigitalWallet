package com.tyson.digitalwallet.ddd.domain.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentOrder {
    private UUID id;
    private UUID walletId;
    private UUID userId;
    private UUID transactionId;
    private BigDecimal amount;
    private PaymentGatewayType gateway;
    private String gatewayOrderId;
    private PaymentOrderStatus status;
    private String checkoutUrl;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public PaymentOrder() {
    }

    public PaymentOrder(
            UUID id,
            UUID walletId,
            UUID userId,
            UUID transactionId,
            BigDecimal amount,
            PaymentGatewayType gateway,
            String gatewayOrderId,
            PaymentOrderStatus status,
            String checkoutUrl,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
        this.id = id;
        this.walletId = walletId;
        this.userId = userId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.gateway = gateway;
        this.gatewayOrderId = gatewayOrderId;
        this.status = status;
        this.checkoutUrl = checkoutUrl;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public static PaymentOrder create(
            UUID walletId,
            UUID userId,
            BigDecimal amount,
            PaymentGatewayType gateway
    ) {
        return new PaymentOrder(
                UUID.randomUUID(),
                walletId,
                userId,
                null,
                amount,
                gateway,
                null,
                PaymentOrderStatus.PENDING,
                null,
                LocalDateTime.now(),
                null
        );
    }

    public void updateCheckoutInfo(String gatewayOrderId, String checkoutUrl) {
        this.gatewayOrderId = gatewayOrderId;
        this.checkoutUrl = checkoutUrl;
    }

    public void markAsSuccess(UUID transactionId) {
        if (this.status != PaymentOrderStatus.PENDING) {
            throw new IllegalStateException("Payment order is already processed with status: " + this.status);
        }
        this.status = PaymentOrderStatus.SUCCESS;
        this.transactionId = transactionId;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        if (this.status != PaymentOrderStatus.PENDING) {
            throw new IllegalStateException("Payment order is already processed with status: " + this.status);
        }
        this.status = PaymentOrderStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsCancelled() {
        if (this.status != PaymentOrderStatus.PENDING) {
            throw new IllegalStateException("Payment order is already processed with status: " + this.status);
        }
        this.status = PaymentOrderStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsExpired() {
        if (this.status != PaymentOrderStatus.PENDING) {
            throw new IllegalStateException("Payment order is already processed with status: " + this.status);
        }
        this.status = PaymentOrderStatus.EXPIRED;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.status == PaymentOrderStatus.PENDING;
    }

    public boolean isSuccess() {
        return this.status == PaymentOrderStatus.SUCCESS;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentGatewayType getGateway() {
        return gateway;
    }

    public void setGateway(PaymentGatewayType gateway) {
        this.gateway = gateway;
    }

    public String getGatewayOrderId() {
        return gatewayOrderId;
    }

    public void setGatewayOrderId(String gatewayOrderId) {
        this.gatewayOrderId = gatewayOrderId;
    }

    public PaymentOrderStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentOrderStatus status) {
        this.status = status;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
