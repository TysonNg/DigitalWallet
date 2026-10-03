package com.tyson.digitalwallet.ddd.domain.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
