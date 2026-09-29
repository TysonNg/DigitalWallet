package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWalletCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;

import java.util.UUID;

public interface CreateWalletUseCase {
    WalletResponse createWallet(UUID userId, String currency);

    default WalletResponse createWallet(CreateWalletCommand command) {
        return createWallet(command.userId(), command.currency());
    }
}
