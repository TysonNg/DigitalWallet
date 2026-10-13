package com.tyson.digitalwallet.ddd.controller.dto.response.beneficiary;

import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.response.BeneficiaryResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record BeneficiaryResponseDto(
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
    public static BeneficiaryResponseDto from(BeneficiaryResponse response) {
        return new BeneficiaryResponseDto(
                response.id(),
                response.userId(),
                response.beneficiaryUserId(),
                response.beneficiaryWalletId(),
                response.nickname(),
                response.fullName(),
                response.phoneNumber(),
                response.email(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
