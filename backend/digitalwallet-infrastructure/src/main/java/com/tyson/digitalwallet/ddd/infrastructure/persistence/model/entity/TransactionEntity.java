package com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TransactionType type;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "amount_before", nullable = false)
    private BigDecimal amountBefore;

    @Column(name = "amount_after", nullable = false)
    private BigDecimal amountAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransactionStatus status;

    @Column(name = "description", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_wallet_id", nullable = true)
    private WalletEntity receiverWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_wallet_id", nullable = true)
    private WalletEntity senderWallet;

    @OneToOne(mappedBy = "transaction", fetch = FetchType.LAZY)
    private IdempotencyKeyEntity idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TransactionEntity() {}

    public TransactionEntity(
            UUID id,
            TransactionType type,
            BigDecimal amount,
            BigDecimal amountBefore,
            BigDecimal amountAfter,
            TransactionStatus status,
            String description,
            WalletEntity receiverWallet,
            WalletEntity senderWallet,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.amountBefore = amountBefore;
        this.amountAfter = amountAfter;
        this.status = status;
        this.description = description;
        this.receiverWallet = receiverWallet;
        this.senderWallet = senderWallet;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmountBefore() {
        return amountBefore;
    }

    public void setAmountBefore(BigDecimal amountBefore) {
        this.amountBefore = amountBefore;
    }

    public BigDecimal getAmountAfter() {
        return amountAfter;
    }

    public void setAmountAfter(BigDecimal amountAfter) {
        this.amountAfter = amountAfter;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WalletEntity getReceiverWallet() {
        return receiverWallet;
    }

    public void setReceiverWallet(WalletEntity receiverWallet) {
        this.receiverWallet = receiverWallet;
    }

    public WalletEntity getSenderWallet() {
        return senderWallet;
    }

    public void setSenderWallet(WalletEntity senderWallet) {
        this.senderWallet = senderWallet;
    }

    public IdempotencyKeyEntity getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(IdempotencyKeyEntity idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
