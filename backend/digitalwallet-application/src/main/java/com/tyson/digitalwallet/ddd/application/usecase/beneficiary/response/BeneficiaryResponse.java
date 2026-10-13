package com.tyson.digitalwallet.ddd.application.usecase.beneficiary.response;

import com.tyson.digitalwallet.ddd.domain.model.entity.Beneficiary;

import java.time.LocalDateTime;
import java.util.UUID;

public record BeneficiaryResponse(
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
    public static BeneficiaryResponse from(Beneficiary b) {
        return new BeneficiaryResponse(
                b.getId(),
                b.getUserId(),
                b.getBeneficiaryUserId(),
                b.getBeneficiaryWalletId(),
                b.getNickname(),
                b.getFullName(),
                b.getPhoneNumber(),
                b.getEmail(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
