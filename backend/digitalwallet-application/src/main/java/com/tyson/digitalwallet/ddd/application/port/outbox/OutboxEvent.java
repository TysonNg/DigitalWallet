package com.tyson.digitalwallet.ddd.application.port.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Integration message envelope. Payload must be JSON-serializable and must not contain secrets. */
public record OutboxEvent(
        UUID id,
        String aggregateType,
        String aggregateId,
        String eventType,
        int schemaVersion,
        String destination,
        Object payload,
        Instant createdAt) {
    public OutboxEvent {
        Objects.requireNonNull(id, "Event ID is required");
        Objects.requireNonNull(createdAt, "Creation time is required");
        Objects.requireNonNull(payload, "Payload is required");
        requireText(aggregateType, 100, "Aggregate type");
        requireText(aggregateId, 255, "Aggregate ID");
        requireText(eventType, 100, "Event type");
        requireText(destination, 100, "Destination alias");
        if (schemaVersion < 1)
            throw new IllegalArgumentException("Schema version must be positive");
    }

    public static OutboxEvent create(
            String aggregateType,
            String aggregateId,
            String eventType,
            int schemaVersion,
            String destination,
            Object payload) {
        return new OutboxEvent(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventType,
                schemaVersion,
                destination,
                payload,
                Instant.now());
    }

    private static void requireText(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max)
            throw new IllegalArgumentException(field + " must contain 1-" + max + " characters");
    }
}
