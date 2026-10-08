package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.IdempotencyKey;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository {
    Optional<IdempotencyKey> findByUserIdAndKey(UUID userId, String idempotencyKey);
    IdempotencyKey save(IdempotencyKey idempotencyKey);
}
