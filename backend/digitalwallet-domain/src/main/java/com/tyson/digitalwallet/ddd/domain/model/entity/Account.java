package com.tyson.digitalwallet.ddd.domain.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.AccountType;

import java.time.LocalDateTime;
import java.util.UUID;

public class Account {
    private UUID id;
    private String code;
    private String name;
    private AccountType type;
    private String currency;
    private UUID walletId;
    private LocalDateTime createdAt;

    public Account() {
    }

    public Account(UUID id, String code, String name, AccountType type, String currency, UUID walletId, LocalDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.currency = (currency != null && !currency.isBlank()) ? currency : "VND";
        this.walletId = walletId;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public static Account createWalletAccount(UUID walletId, String currency) {
        return new Account(
                UUID.randomUUID(),
                "WALLET_" + walletId.toString(),
                "Ví người dùng " + walletId,
                AccountType.LIABILITY,
                currency,
                walletId,
                LocalDateTime.now()
        );
    }

    public boolean isSystemAccount() {
        return this.walletId == null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
