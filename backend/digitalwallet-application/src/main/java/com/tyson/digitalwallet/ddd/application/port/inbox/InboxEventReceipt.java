package com.tyson.digitalwallet.ddd.application.port.inbox;

import java.time.Instant;
import java.util.*;

/** Durable completion receipt, shared by consumers without transport-specific dependencies. */
public record InboxEventReceipt(
        String consumerName, UUID eventId, String aggregateId, Instant processedAt) {
    public InboxEventReceipt {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(processedAt);
        if (consumerName == null
                || consumerName.isBlank()
                || consumerName.length() > 100
                || aggregateId == null
                || aggregateId.isBlank()
                || aggregateId.length() > 255)
            throw new IllegalArgumentException("Invalid inbox identity");
    }
}
