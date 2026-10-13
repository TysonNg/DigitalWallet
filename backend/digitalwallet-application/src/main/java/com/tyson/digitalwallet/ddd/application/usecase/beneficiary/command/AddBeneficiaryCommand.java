package com.tyson.digitalwallet.ddd.application.usecase.beneficiary.command;

import java.util.UUID;

public record AddBeneficiaryCommand(
        UUID userId,
        UUID beneficiaryUserId,
        UUID beneficiaryWalletId,
        String nickname
) {
}
