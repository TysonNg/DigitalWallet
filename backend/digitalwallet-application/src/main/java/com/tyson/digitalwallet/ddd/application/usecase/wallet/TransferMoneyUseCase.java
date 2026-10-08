package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.TransferMoneyCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;

public interface TransferMoneyUseCase {
    WalletResponse transfer(TransferMoneyCommand transferMoneyCommand);
}
