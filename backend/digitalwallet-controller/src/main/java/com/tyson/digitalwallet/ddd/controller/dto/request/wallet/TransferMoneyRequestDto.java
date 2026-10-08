package com.tyson.digitalwallet.ddd.controller.dto.request.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.TransferMoneyCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferMoneyRequestDto(
        @NotNull(message = "Sender wallet ID is required")
        UUID senderWalletId,

        @NotNull(message = "Receiver wallet ID is required")
        UUID receiverWalletId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Transfer amount must be greater than zero")
        BigDecimal amount,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        String idempotencyKey
) {
    public TransferMoneyCommand toCommand() {
        return new TransferMoneyCommand(
                senderWalletId,
                receiverWalletId,
                amount,
                description,
                idempotencyKey
        );
    }
}
