package com.tyson.digitalwallet.ddd.application.usecase.wallet;

import java.util.UUID;

public interface ConsumeWithdrawalEventUseCase {
    void consume(UUID eventId, UUID withdrawalId);
}
