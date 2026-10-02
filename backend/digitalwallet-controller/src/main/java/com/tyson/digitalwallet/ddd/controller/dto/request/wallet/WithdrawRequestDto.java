package com.tyson.digitalwallet.ddd.controller.dto.request.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.WithdrawCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawRequestDto(
        @NotNull UUID walletId,
        @NotNull @DecimalMin(value = "0.01", message = "Withdraw amount must be greater than 0") BigDecimal amount
) {
    public WithdrawCommand toCommand() {
        return new WithdrawCommand(walletId, amount);
    }
}
