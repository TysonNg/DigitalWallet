package com.tyson.digitalwallet.ddd.application.usecase.beneficiary.command;

import java.util.UUID;

public record UpdateBeneficiaryCommand(
        UUID beneficiaryId,
        UUID userId,
        String nickname
) {
}
