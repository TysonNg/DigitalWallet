package com.tyson.digitalwallet.ddd.application.usecase.wallet.response;

import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID userId,
        BigDecimal balance,
        String currency,
        WalletStatus status,
        LocalDateTime created_at,
        LocalDateTime updated_at
) {
    public static WalletResponse from(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getCreated_at(),
                wallet.getUpdated_at()
        );
    }
}
