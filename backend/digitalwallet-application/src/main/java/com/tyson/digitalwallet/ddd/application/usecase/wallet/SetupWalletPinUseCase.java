package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.SetupWalletPinCommand;

public interface SetupWalletPinUseCase {
    void setupPin(SetupWalletPinCommand command);
}
