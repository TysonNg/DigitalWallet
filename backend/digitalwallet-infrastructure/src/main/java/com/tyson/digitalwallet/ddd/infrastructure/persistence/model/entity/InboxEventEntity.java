package com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity;

import jakarta.persistence.*;

import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "inbox_event")
@IdClass(InboxEventEntity.Key.class)
public class InboxEventEntity implements Persistable<InboxEventEntity.Key> {
    @Id
    @Column(name = "consumer_name", nullable = false, length = 100, updatable = false)
    private String consumerName;

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false, length = 255, updatable = false)
    private String aggregateId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @Transient private boolean newEntity = true;

    public InboxEventEntity() {}

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String value) {
        consumerName = value;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID value) {
        eventId = value;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String value) {
        aggregateId = value;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant value) {
        processedAt = value;
    }

    @Override
    @Transient
    public Key getId() {
        return new Key(consumerName, eventId);
    }

    @Override
    @Transient
    public boolean isNew() {
        return newEntity;
    }

    @PostPersist
    @PostLoad
    private void markPersisted() {
        newEntity = false;
    }

    public static class Key implements Serializable {
        public String consumerName;
        public UUID eventId;

        public Key() {}

        public Key(String consumerName, UUID eventId) {
            this.consumerName = consumerName;
            this.eventId = eventId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key k
                    && Objects.equals(consumerName, k.consumerName)
                    && Objects.equals(eventId, k.eventId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(consumerName, eventId);
        }
    }
}
