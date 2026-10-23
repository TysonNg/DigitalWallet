package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import java.util.UUID;

public interface ProcessWithdrawalUseCase {
    void process(UUID id);
}
