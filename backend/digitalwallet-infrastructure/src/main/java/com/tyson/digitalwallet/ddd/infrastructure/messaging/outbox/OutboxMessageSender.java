package com.tyson.digitalwallet.ddd.infrastructure.messaging.outbox;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.OutboxEventEntity;

/** Returns only after the transport confirms delivery; an exception leaves the event retryable. */
@FunctionalInterface
public interface OutboxMessageSender {
    void send(OutboxEventEntity event) throws Exception;
}
