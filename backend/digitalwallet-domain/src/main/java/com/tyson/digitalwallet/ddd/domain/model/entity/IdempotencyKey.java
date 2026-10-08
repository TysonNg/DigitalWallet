package com.tyson.digitalwallet.ddd.domain.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.IdempotencyKeyStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class IdempotencyKey {
    private UUID id;
    private UUID userId;
    private String idempotencyKey;
    private UUID transactionId;
    private String requestHash;
    private IdempotencyKeyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public IdempotencyKey(
            UUID id,
            UUID userId,
            String idempotencyKey,
            UUID transactionId,
            String requestHash,
            IdempotencyKeyStatus status,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        this.id = id;
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.transactionId = transactionId;
        this.requestHash = requestHash;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static IdempotencyKey startProcessing(UUID userId, String idempotencyKey, String requestHash) {
        return new IdempotencyKey(
                UUID.randomUUID(),
                userId,
                idempotencyKey,
                null,
                requestHash,
                IdempotencyKeyStatus.PROCESSING,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(24)
        );
    }

    public void complete(UUID transactionId) {
        this.transactionId = transactionId;
        this.status = IdempotencyKeyStatus.COMPLETED;
    }

    public void fail() {
        this.status = IdempotencyKeyStatus.FAILED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isCompleted() {
        return status == IdempotencyKeyStatus.COMPLETED;
    }

    public boolean isProcessing() {
        return status == IdempotencyKeyStatus.PROCESSING;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public IdempotencyKeyStatus getStatus() {
        return status;
    }

    public void setStatus(IdempotencyKeyStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
