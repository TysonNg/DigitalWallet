package com.tyson.digitalwallet.ddd.application.usecase.ai.response;

import java.math.BigDecimal;
import java.util.UUID;

public record DraftTransferPayload(
        UUID recipientUserId,
        UUID recipientWalletId,
        String recipientName,
        String recipientPhoneOrEmail,
        BigDecimal amount,
        String note,
        BigDecimal fee,
        boolean canProceed,
        String message
) {
}
