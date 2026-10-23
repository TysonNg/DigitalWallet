package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.PayoutAttempt;

import java.util.*;

public interface PayoutAttemptRepository {
    Optional<PayoutAttempt> findByWithdrawalId(UUID withdrawalId);

    PayoutAttempt save(PayoutAttempt attempt);
}
