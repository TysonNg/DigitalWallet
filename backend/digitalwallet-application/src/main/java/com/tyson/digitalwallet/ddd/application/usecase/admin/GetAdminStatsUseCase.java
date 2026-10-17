package com.tyson.digitalwallet.ddd.application.usecase.admin;

import com.tyson.digitalwallet.ddd.application.usecase.admin.response.AdminStatsResponse;

public interface GetAdminStatsUseCase {
    AdminStatsResponse getSystemStats();
}
