package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;

import java.util.UUID;

public interface GetWalletUseCase {
    WalletResponse getWalletById(UUID id);
    WalletResponse getWalletByUserId(UUID userId);
}
