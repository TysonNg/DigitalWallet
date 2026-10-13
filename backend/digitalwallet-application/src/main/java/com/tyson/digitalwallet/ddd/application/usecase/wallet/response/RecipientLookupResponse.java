package com.tyson.digitalwallet.ddd.application.usecase.wallet.response;

import java.util.UUID;

public record RecipientLookupResponse(
        UUID userId,
        UUID walletId,
        String fullName,
        String phoneNumber,
        String email,
        String currency
) {
}
