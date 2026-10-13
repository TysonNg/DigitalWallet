package com.tyson.digitalwallet.ddd.controller.dto.request.beneficiary;

import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.command.AddBeneficiaryCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddBeneficiaryRequestDto(
        @NotNull(message = "Beneficiary user ID is required")
        UUID beneficiaryUserId,

        @NotNull(message = "Beneficiary wallet ID is required")
        UUID beneficiaryWalletId,

        @Size(max = 100, message = "Nickname must not exceed 100 characters")
        String nickname
) {
    public AddBeneficiaryCommand toCommand(UUID currentUserId) {
        return new AddBeneficiaryCommand(
                currentUserId,
                beneficiaryUserId,
                beneficiaryWalletId,
                nickname
        );
    }
}
