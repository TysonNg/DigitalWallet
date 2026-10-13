package com.tyson.digitalwallet.ddd.controller.dto.response.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.RecipientLookupResponse;

import java.util.UUID;

public record RecipientLookupResponseDto(
        UUID userId,
        UUID walletId,
        String fullName,
        String phoneNumber,
        String email,
        String currency
) {
    public static RecipientLookupResponseDto from(RecipientLookupResponse response) {
        return new RecipientLookupResponseDto(
                response.userId(),
                response.walletId(),
                response.fullName(),
                response.phoneNumber(),
                response.email(),
                response.currency()
        );
    }
}
