package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WithdrawalResponse;

import java.util.*;

public interface GetWithdrawalHistoryUseCase {
    List<WithdrawalResponse> history(UUID userId, UUID walletId);
}
