package com.tyson.digitalwallet.ddd.application.usecase.admin;

import com.tyson.digitalwallet.ddd.application.usecase.admin.command.ToggleUserStatusCommand;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminUserSummaryResponse;

public interface ToggleUserStatusUseCase {
    AdminUserSummaryResponse toggleStatus(ToggleUserStatusCommand command);
}
