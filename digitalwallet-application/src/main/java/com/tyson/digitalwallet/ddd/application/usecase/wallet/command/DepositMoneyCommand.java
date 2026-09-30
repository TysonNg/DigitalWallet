package com.tyson.digitalwallet.ddd.application.usecase.wallet.command;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositMoneyCommand(
        UUID walletId,
        BigDecimal amount
) {
}
