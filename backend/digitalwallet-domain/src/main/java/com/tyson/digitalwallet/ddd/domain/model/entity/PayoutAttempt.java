package com.tyson.digitalwallet.ddd.domain.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.PayoutAttemptStatus;

import java.time.Instant;
import java.util.*;

/** One logical execution; transport retries reuse its identity and checkpoints. */
public class PayoutAttempt {
    private final UUID id, withdrawalId;
    private final String provider, currency;
    private final long amount;
    private final Map<String, String> metadata;
    private final Instant createdAt;
    private final PayoutAttemptStatus status;
    private final String error;

    public PayoutAttempt(
            UUID id,
            UUID withdrawalId,
            String provider,
            long amount,
            String currency,
            Map<String, String> metadata,
            Instant createdAt) {
        this(
                id,
                withdrawalId,
                provider,
                amount,
                currency,
                metadata,
                createdAt,
                PayoutAttemptStatus.PENDING,
                null);
    }

    public PayoutAttempt(
            UUID id,
            UUID withdrawalId,
            String provider,
            long amount,
            String currency,
            Map<String, String> metadata,
            Instant createdAt,
            PayoutAttemptStatus status,
            String error) {
        this.status = status;
        this.error = error;
        this.id = id;
        this.withdrawalId = withdrawalId;
        this.provider = provider;
        this.amount = amount;
        this.currency = currency;
        this.metadata = Map.copyOf(metadata);
        this.createdAt = createdAt;
    }

    public static PayoutAttempt create(
            UUID withdrawalId, String provider, long amount, String currency) {
        return new PayoutAttempt(
                UUID.randomUUID(),
                withdrawalId,
                provider,
                amount,
                currency,
                Map.of(),
                Instant.now());
    }

    public PayoutAttempt checkpoint(String key, String value) {
        Map<String, String> next = new HashMap<>(metadata);
        next.put(key, value);
        return new PayoutAttempt(
                id, withdrawalId, provider, amount, currency, next, createdAt, status, error);
    }

    public PayoutAttempt withStatus(PayoutAttemptStatus next, String detail) {
        Objects.requireNonNull(next);
        return new PayoutAttempt(
                id, withdrawalId, provider, amount, currency, metadata, createdAt, next, detail);
    }

    public PayoutAttemptStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWithdrawalId() {
        return withdrawalId;
    }

    public String getProvider() {
        return provider;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
