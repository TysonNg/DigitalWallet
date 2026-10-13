package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.RecipientLookupResponse;

import java.util.UUID;

public interface LookupRecipientUseCase {
    RecipientLookupResponse lookup(UUID currentUserId, String identifier);
}
