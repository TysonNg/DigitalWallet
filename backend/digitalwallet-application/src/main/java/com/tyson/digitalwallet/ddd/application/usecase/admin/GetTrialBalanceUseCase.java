package com.tyson.digitalwallet.ddd.application.usecase.admin;

import com.tyson.digitalwallet.ddd.application.usecase.admin.response.TrialBalanceResponse;

public interface GetTrialBalanceUseCase {
    TrialBalanceResponse getTrialBalance();
}
