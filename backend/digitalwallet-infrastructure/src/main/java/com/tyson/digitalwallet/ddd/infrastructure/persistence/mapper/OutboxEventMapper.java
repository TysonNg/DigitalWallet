package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.port.outbox.OutboxEvent;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.OutboxEventEntity;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class OutboxEventMapper {
    private final ObjectMapper json;

    public OutboxEventMapper(ObjectMapper json) {
        this.json = json;
    }

    public OutboxEventEntity toEntity(OutboxEvent event) {
        Objects.requireNonNull(event, "Outbox event is required");
        var entity = new OutboxEventEntity();
        entity.setId(event.id());
        entity.setAggregateType(event.aggregateType());
        entity.setAggregateId(event.aggregateId());
        entity.setEventType(event.eventType());
        entity.setSchemaVersion(event.schemaVersion());
        entity.setDestination(event.destination());
        try {
            entity.setPayload(json.writeValueAsString(event.payload()));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Outbox payload cannot be serialized as JSON", ex);
        }
        entity.setCreatedAt(event.createdAt());
        entity.setAvailableAt(event.createdAt());
        entity.setAttempts(0);
        return entity;
    }
}
