package com.tyson.digitalwallet.ddd.controller.dto.request.wallet;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateWithdrawalRequestDto(
        @NotNull UUID walletId,
        @NotNull @DecimalMin("0.0001") BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{8,100}") String idempotencyKey,
        String provider,
        String destination,
        String testBank,
        String pin) {}
