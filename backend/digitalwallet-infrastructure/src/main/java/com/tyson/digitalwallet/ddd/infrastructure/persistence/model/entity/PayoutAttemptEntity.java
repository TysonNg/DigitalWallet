package com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.PayoutAttemptStatus;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "payout_attempts")
public class PayoutAttemptEntity {
    @Id private UUID id;

    @Column(name = "withdrawal_id", nullable = false)
    private UUID withdrawalId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo = 1;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutAttemptStatus status;

    @Column(length = 500)
    private String error;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "provider_account_reference")
    private String providerAccountReference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_metadata", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PayoutAttemptEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    public UUID getWithdrawalId() {
        return withdrawalId;
    }

    public void setWithdrawalId(UUID value) {
        this.withdrawalId = value;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(int value) {
        this.attemptNo = value;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String value) {
        this.provider = value;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long value) {
        this.amount = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String value) {
        this.currency = value;
    }

    public PayoutAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(PayoutAttemptStatus value) {
        this.status = value;
    }

    public String getError() {
        return error;
    }

    public void setError(String value) {
        this.error = value;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String value) {
        this.providerReference = value;
    }

    public String getProviderAccountReference() {
        return providerAccountReference;
    }

    public void setProviderAccountReference(String value) {
        this.providerAccountReference = value;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> value) {
        this.metadata = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }
}
