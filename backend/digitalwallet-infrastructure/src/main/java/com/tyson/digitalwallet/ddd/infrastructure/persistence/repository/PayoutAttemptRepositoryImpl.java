package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.PayoutAttempt;
import com.tyson.digitalwallet.ddd.domain.repository.PayoutAttemptRepository;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.PayoutAttemptMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.PayoutAttemptEntity;

import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class PayoutAttemptRepositoryImpl implements PayoutAttemptRepository {
    private final SpringDataPayoutAttemptRepository repository;
    private final PayoutAttemptMapper mapper;

    public PayoutAttemptRepositoryImpl(
            SpringDataPayoutAttemptRepository repository, PayoutAttemptMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<PayoutAttempt> findByWithdrawalId(UUID id) {
        return repository.findTopByWithdrawalIdOrderByAttemptNoDesc(id).map(mapper::toDomain);
    }

    @Override
    public PayoutAttempt save(PayoutAttempt attempt) {
        var entity = repository.findById(attempt.getId()).orElseGet(PayoutAttemptEntity::new);
        return mapper.toDomain(repository.save(mapper.toEntity(attempt, entity)));
    }
}
