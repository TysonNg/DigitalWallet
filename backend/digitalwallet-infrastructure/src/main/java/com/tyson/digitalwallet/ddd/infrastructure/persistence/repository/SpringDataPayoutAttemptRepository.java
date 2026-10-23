package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.PayoutAttemptEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface SpringDataPayoutAttemptRepository
        extends JpaRepository<PayoutAttemptEntity, UUID> {
    Optional<PayoutAttemptEntity> findTopByWithdrawalIdOrderByAttemptNoDesc(UUID withdrawalId);
}
