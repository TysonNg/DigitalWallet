package com.tyson.digitalwallet.ddd.controller.dto.request.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWalletCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateWalletRequestDto(
        String currency,
        @NotNull UUID userId
        ) {
    public CreateWalletCommand toCommand() {
        return new CreateWalletCommand(userId,currency);
    }
}
