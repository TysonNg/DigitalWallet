package com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "beneficiaries")
public class BeneficiaryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_user_id", nullable = false)
    private UserEntity beneficiaryUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_wallet_id", nullable = false)
    private WalletEntity beneficiaryWallet;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BeneficiaryEntity() {
    }

    public BeneficiaryEntity(
            UUID id,
            UserEntity user,
            UserEntity beneficiaryUser,
            WalletEntity beneficiaryWallet,
            String nickname,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.user = user;
        this.beneficiaryUser = beneficiaryUser;
        this.beneficiaryWallet = beneficiaryWallet;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public UserEntity getBeneficiaryUser() {
        return beneficiaryUser;
    }

    public void setBeneficiaryUser(UserEntity beneficiaryUser) {
        this.beneficiaryUser = beneficiaryUser;
    }

    public WalletEntity getBeneficiaryWallet() {
        return beneficiaryWallet;
    }

    public void setBeneficiaryWallet(WalletEntity beneficiaryWallet) {
        this.beneficiaryWallet = beneficiaryWallet;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
