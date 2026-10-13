package com.tyson.digitalwallet.ddd.controller.dto.request.beneficiary;

import com.tyson.digitalwallet.ddd.application.usecase.beneficiary.command.UpdateBeneficiaryCommand;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateBeneficiaryRequestDto(
        @Size(max = 100, message = "Nickname must not exceed 100 characters")
        String nickname
) {
    public UpdateBeneficiaryCommand toCommand(UUID beneficiaryId, UUID currentUserId) {
        return new UpdateBeneficiaryCommand(
                beneficiaryId,
                currentUserId,
                nickname
        );
    }
}
