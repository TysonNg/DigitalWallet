package com.tyson.digitalwallet.ddd.controller.dto.request.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.DepositMoneyCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositMoneyRequestDto(
        @NotNull UUID walletId,
        @NotNull @DecimalMin(value = "0.01", message = "Deposit amount must be greater than 0")BigDecimal amount
        ) {
    public DepositMoneyCommand toCommand(){
        return new DepositMoneyCommand(walletId, amount);
    }
}
