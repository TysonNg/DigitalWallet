package com.tyson.digitalwallet.ddd.domain.event;

import java.time.Instant;
import java.util.UUID;

public record UserLoggedInEvent(
        UUID userId,
        String email,
        String sessionId,
        Instant occurredAt
) implements DomainEvent {
}
