package com.tyson.digitalwallet.ddd.controller.dto.response.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.user.response.UserResponse;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;

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
    public static WalletResponseDto from(WalletResponse response) {
        if(response == null) {
            return null;
        }

        return new WalletResponseDto(
                response.id(),
                response.balance(),
                response.currency(),
                response.created_at(),
                response.updated_at()
        );
    }
}
