package com.tyson.digitalwallet.ddd.application.usecase.admin;

import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminUserSummaryResponse;

import java.util.List;

public interface GetAdminUsersUseCase {
    List<AdminUserSummaryResponse> getAllUsers();
}
