package com.tyson.digitalwallet.ddd.application.usecase.wallet.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateWithdrawalCommand(
        UUID userId,
        UUID walletId,
        BigDecimal amount,
        String idempotencyKey,
        String provider,
        String destination,
        String pin) {}
