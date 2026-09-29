package com.tyson.digitalwallet.ddd.domain.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Wallet {
    private UUID id;
    private UUID userId;
    private BigDecimal balance;
    private String currency;
    private WalletStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Wallet(UUID id, UUID userId ,BigDecimal balance, String currency, WalletStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreated_at() {
        return createdAt;
    }

    public LocalDateTime getUpdated_at() {
        return updatedAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setStatus(WalletStatus status) {
        this.status = status;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.createdAt = created_at;
    }

    public void setUpdated_at(LocalDateTime updated_at) {
        this.updatedAt = updated_at;
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "id=" + id +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                ", created_at=" + createdAt +
                ", updated_at=" + updatedAt +
                '}';
    }

    public void activate(){
        if(status == WalletStatus.CLOSED) {
            throw new IllegalStateException(
                    "Closed wallet cannot be activated"
            );
        }
        status = WalletStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        if(status == WalletStatus.CLOSED){
            throw new IllegalStateException(
                    "This wallet already closed"
            );
        }
        status = WalletStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    public void frozen() {
        if(status == WalletStatus.CLOSED) {
            throw new IllegalStateException(
                    "You can't frozen cause wallet was closed"
            );
        }
        status = WalletStatus.FROZEN;
        this.updatedAt = LocalDateTime.now();
    }

    public void deposit(BigDecimal amount) {
        if(status != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Cannot deposit: Wallet is " + this.status);
        }
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Deposit amount must be greater than zero");
        }

        this.balance = balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void withdraw(BigDecimal amount) {
        if(status != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Cannot deposit: Wallet is " + this.status);
        }

        if(balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance. Available: " + this.balance + ", Required: " + amount);
        }

        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Withdraw amount must be greater than zero");
        }

        this.balance = balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }
}
