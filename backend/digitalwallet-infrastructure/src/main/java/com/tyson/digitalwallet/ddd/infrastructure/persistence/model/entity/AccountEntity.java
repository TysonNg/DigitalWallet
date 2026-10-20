package com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.AccountType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AccountType type;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", unique = true)
    private WalletEntity wallet;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AccountEntity() {
    }

    public AccountEntity(UUID id, String code, String name, AccountType type, String currency, WalletEntity wallet, LocalDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.wallet = wallet;
        this.createdAt = createdAt;
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

    public WalletEntity getWallet() {
        return wallet;
    }

    public void setWallet(WalletEntity wallet) {
        this.wallet = wallet;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
