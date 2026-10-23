package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.CreateWithdrawalCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WithdrawalResponse;

public interface CreateWithdrawalUseCase {
    WithdrawalResponse create(CreateWithdrawalCommand command);
}
