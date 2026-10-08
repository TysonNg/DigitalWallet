package com.tyson.digitalwallet.ddd.application.usecase.wallet.command;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferMoneyCommand(
        UUID senderWalletId,
        UUID receiverWalletId,
        BigDecimal amount,
        String description,
        String idempotencyKey
) {
    public TransferMoneyCommand(
            UUID senderWalletId,
            UUID receiverWalletId,
            BigDecimal amount,
            String description
    ) {
        this(senderWalletId, receiverWalletId, amount, description, null);
    }
}
