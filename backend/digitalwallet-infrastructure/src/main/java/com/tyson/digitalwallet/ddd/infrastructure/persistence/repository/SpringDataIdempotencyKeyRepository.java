package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataIdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {
    Optional<IdempotencyKeyEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    void deleteByExpiresAtBefore(LocalDateTime time);
}
