package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.DepositMoneyCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;

public interface DepositMoneyUseCase {
    WalletResponse deposit(DepositMoneyCommand command);
}
