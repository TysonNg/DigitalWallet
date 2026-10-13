package com.tyson.digitalwallet.ddd.domain.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Beneficiary {
    private UUID id;
    private UUID userId;
    private UUID beneficiaryUserId;
    private UUID beneficiaryWalletId;
    private String nickname;
    private String fullName;
    private String phoneNumber;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Beneficiary() {
    }

    public Beneficiary(
            UUID id,
            UUID userId,
            UUID beneficiaryUserId,
            UUID beneficiaryWalletId,
            String nickname,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, userId, beneficiaryUserId, beneficiaryWalletId, nickname, null, null, null, createdAt, updatedAt);
    }

    public Beneficiary(
            UUID id,
            UUID userId,
            UUID beneficiaryUserId,
            UUID beneficiaryWalletId,
            String nickname,
            String fullName,
            String phoneNumber,
            String email,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.beneficiaryUserId = beneficiaryUserId;
        this.beneficiaryWalletId = beneficiaryWalletId;
        this.nickname = nickname;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Beneficiary create(
            UUID userId,
            UUID beneficiaryUserId,
            UUID beneficiaryWalletId,
            String nickname
    ) {
        if (userId == null || beneficiaryUserId == null || beneficiaryWalletId == null) {
            throw new IllegalArgumentException("User ID, beneficiary user ID, and beneficiary wallet ID must not be null");
        }
        if (userId.equals(beneficiaryUserId)) {
            throw new IllegalArgumentException("Cannot add yourself as a beneficiary");
        }
        String cleanNickname = nickname != null && !nickname.isBlank() ? nickname.trim() : null;
        LocalDateTime now = LocalDateTime.now();
        return new Beneficiary(
                UUID.randomUUID(),
                userId,
                beneficiaryUserId,
                beneficiaryWalletId,
                cleanNickname,
                now,
                now
        );
    }

    public void updateNickname(String newNickname) {
        this.nickname = newNickname != null && !newNickname.isBlank() ? newNickname.trim() : null;
        this.updatedAt = LocalDateTime.now();
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

    public UUID getBeneficiaryUserId() {
        return beneficiaryUserId;
    }

    public void setBeneficiaryUserId(UUID beneficiaryUserId) {
        this.beneficiaryUserId = beneficiaryUserId;
    }

    public UUID getBeneficiaryWalletId() {
        return beneficiaryWalletId;
    }

    public void setBeneficiaryWalletId(UUID beneficiaryWalletId) {
        this.beneficiaryWalletId = beneficiaryWalletId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Beneficiary that = (Beneficiary) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
