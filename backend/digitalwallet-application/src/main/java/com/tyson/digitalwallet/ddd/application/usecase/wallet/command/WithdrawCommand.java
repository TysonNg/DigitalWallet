package com.tyson.digitalwallet.ddd.application.usecase.wallet.command;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawCommand(
        UUID walletId,
        BigDecimal amount
) {
}
