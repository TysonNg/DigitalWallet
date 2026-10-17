package com.tyson.digitalwallet.ddd.application.usecase.admin;

import com.tyson.digitalwallet.ddd.application.usecase.admin.command.ToggleWalletFreezeCommand;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminUserSummaryResponse;

public interface ToggleWalletFreezeUseCase {
    AdminUserSummaryResponse toggleFreeze(ToggleWalletFreezeCommand command);
}
