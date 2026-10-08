package com.tyson.digitalwallet.ddd.domain.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {
    private UUID id;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal amountBefore;
    private BigDecimal amountAfter;
    private TransactionStatus status;
    private String description;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private LocalDateTime createdAt;

    public Transaction(
            UUID id,
            TransactionType type,
            BigDecimal amount,
            BigDecimal amountBefore,
            BigDecimal amountAfter,
            TransactionStatus status,
            String description,
            UUID senderWalletId,
            UUID receiverWalletId,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.amountBefore = amountBefore;
        this.amountAfter = amountAfter;
        this.status = status;
        this.description = description;
        this.senderWalletId = senderWalletId;
        this.receiverWalletId = receiverWalletId;
        this.createdAt = createdAt;
    }

    public static Transaction createTransfer(
            UUID senderWalletId,
            UUID receiverWalletId,
            BigDecimal amount,
            BigDecimal senderBalanceBefore,
            BigDecimal senderBalanceAfter,
            String description
    ) {
        return new Transaction(
                UUID.randomUUID(),
                TransactionType.TRANSFER_OUT,
                amount,
                senderBalanceBefore,
                senderBalanceAfter,
                TransactionStatus.SUCCESS,
                description,
                senderWalletId,
                receiverWalletId,
                LocalDateTime.now()
        );
    }

    public static Transaction createDeposit(
            UUID walletId,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter
    ) {
        return new Transaction(
                UUID.randomUUID(),
                TransactionType.DEPOSIT,
                amount,
                balanceBefore,
                balanceAfter,
                TransactionStatus.SUCCESS,
                "Deposit to wallet",
                null,
                walletId,
                LocalDateTime.now()
        );
    }

    public static Transaction createWithdraw(
            UUID walletId,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter
    ) {
        return new Transaction(
                UUID.randomUUID(),
                TransactionType.WITHDRAW,
                amount,
                balanceBefore,
                balanceAfter,
                TransactionStatus.SUCCESS,
                "Withdraw from wallet",
                walletId,
                null,
                LocalDateTime.now()
        );
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

    public UUID getSenderWalletId() {
        return senderWalletId;
    }

    public void setSenderWalletId(UUID senderWalletId) {
        this.senderWalletId = senderWalletId;
    }

    public UUID getReceiverWalletId() {
        return receiverWalletId;
    }

    public void setReceiverWalletId(UUID receiverWalletId) {
        this.receiverWalletId = receiverWalletId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
