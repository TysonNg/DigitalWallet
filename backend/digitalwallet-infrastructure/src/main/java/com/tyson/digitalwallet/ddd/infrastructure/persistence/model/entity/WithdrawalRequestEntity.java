package com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "withdrawal_requests")
public class WithdrawalRequestEntity {
    @Id private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 19, scale = 4, nullable = false)
    private BigDecimal balanceBefore;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String destination;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(length = 500)
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WithdrawalRequestEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID value) {
        this.walletId = value;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID value) {
        this.userId = value;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal value) {
        this.amount = value;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal value) {
        this.balanceBefore = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String value) {
        this.currency = value;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String value) {
        this.destination = value;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String value) {
        this.idempotencyKey = value;
    }

    public WithdrawalStatus getStatus() {
        return status;
    }

    public void setStatus(WithdrawalStatus value) {
        this.status = value;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID value) {
        this.transactionId = value;
    }

    public String getError() {
        return error;
    }

    public void setError(String value) {
        this.error = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant value) {
        this.updatedAt = value;
    }
}
