package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.WithdrawCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;

import java.util.UUID;

public interface WithdrawUseCase {
    WalletResponse withdraw(WithdrawCommand command);
}
