package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.OutboxEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @org.springframework.data.jpa.repository.Query(
            value =
                    "SELECT * FROM outbox_event WHERE published_at IS NULL AND available_at <= :now"
                        + " ORDER BY available_at, created_at, id LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    java.util.Optional<OutboxEventEntity> lockNextAvailable(
            @org.springframework.data.repository.query.Param("now") java.time.Instant now);
}
