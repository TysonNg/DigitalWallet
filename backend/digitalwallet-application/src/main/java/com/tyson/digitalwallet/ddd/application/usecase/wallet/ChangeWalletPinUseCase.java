package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.ChangeWalletPinCommand;

public interface ChangeWalletPinUseCase {
    void changePin(ChangeWalletPinCommand command);
}
