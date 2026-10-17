package com.tyson.digitalwallet.ddd.application.usecase.admin;

import com.tyson.digitalwallet.ddd.application.usecase.admin.command.AssignUserRolesCommand;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminUserSummaryResponse;

public interface AssignUserRolesUseCase {
    AdminUserSummaryResponse assignRoles(AssignUserRolesCommand command);
}
