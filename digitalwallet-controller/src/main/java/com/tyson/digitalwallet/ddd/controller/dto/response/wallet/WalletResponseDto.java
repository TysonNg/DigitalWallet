package com.tyson.digitalwallet.ddd.controller.dto.response.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletResponseDto(
        UUID id,
        BigDecimal balance,
        String currency,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public WalletResponseDto(WalletResponse r) {
        this(
                r.id(),
                r.balance(),
                r.currency(),
                r.created_at(),
                r.updated_at()
        );
    }

    public static WalletResponseDto from(WalletResponse response) {
        return response == null ? null : new WalletResponseDto(response);
    }
}
