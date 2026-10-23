package com.tyson.digitalwallet.ddd.domain.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** A business withdrawal. Provider execution details belong to PayoutAttempt. */
public class WithdrawalRequest {
    private UUID id;
    private UUID walletId;
    private UUID userId;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private String currency;
    private String destination;
    private String idempotencyKey;
    private WithdrawalStatus status;
    private UUID transactionId;
    private String error;
    private Instant createdAt;
    private Instant updatedAt;

    public WithdrawalRequest(
            UUID id,
            UUID walletId,
            UUID userId,
            BigDecimal amount,
            BigDecimal balanceBefore,
            String currency,
            String destination,
            String idempotencyKey,
            WithdrawalStatus status,
            UUID transactionId,
            String error,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.walletId = walletId;
        this.userId = userId;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.currency = currency;
        this.destination = destination;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.transactionId = transactionId;
        this.error = error;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WithdrawalRequest create(
            UUID walletId,
            UUID userId,
            BigDecimal amount,
            BigDecimal balanceBefore,
            String currency,
            String destination,
            String key) {
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Amount must be positive");
        Objects.requireNonNull(walletId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(currency);
        if (destination == null || destination.isBlank())
            throw new IllegalArgumentException("Destination is required");
        Instant now = Instant.now();
        return new WithdrawalRequest(
                UUID.randomUUID(),
                walletId,
                userId,
                amount,
                balanceBefore,
                currency,
                destination,
                key,
                WithdrawalStatus.PENDING,
                null,
                null,
                now,
                now);
    }

    public boolean isHeld() {
        return status == WithdrawalStatus.PENDING
                || status == WithdrawalStatus.PROCESSING
                || status == WithdrawalStatus.REVIEW_REQUIRED;
    }

    public void markProcessing() {
        if (status != WithdrawalStatus.PENDING && status != WithdrawalStatus.PROCESSING)
            throw new IllegalStateException("Withdrawal is not processable");
        status = WithdrawalStatus.PROCESSING;
        updatedAt = Instant.now();
    }

    public void complete(UUID transaction) {
        if (!isHeld()) throw new IllegalStateException("Withdrawal is already resolved");
        transactionId = Objects.requireNonNull(transaction);
        status = WithdrawalStatus.SUCCEEDED;
        error = null;
        updatedAt = Instant.now();
    }

    public void fail(String reason) {
        if (!isHeld()) throw new IllegalStateException("Only held withdrawals can fail");
        status = WithdrawalStatus.FAILED;
        error = reason;
        updatedAt = Instant.now();
    }

    public void markReturned(String reason) {
        if (status != WithdrawalStatus.SUCCEEDED)
            throw new IllegalStateException("Only completed withdrawals can be returned");
        status = WithdrawalStatus.RETURNED;
        error = reason;
        updatedAt = Instant.now();
    }

    public void requireReview(String reason) {
        if (!isHeld())
            throw new IllegalStateException("Resolved withdrawals cannot be put on hold");
        status = WithdrawalStatus.REVIEW_REQUIRED;
        error = reason;
        updatedAt = Instant.now();
    }

    public void recordError(String reason) {
        error = reason;
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDestination() {
        return destination;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public WithdrawalStatus getStatus() {
        return status;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getError() {
        return error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
