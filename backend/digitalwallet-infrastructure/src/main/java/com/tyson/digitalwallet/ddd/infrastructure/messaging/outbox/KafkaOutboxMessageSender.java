package com.tyson.digitalwallet.ddd.infrastructure.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.infrastructure.config.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.OutboxEventEntity;

import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.TimeUnit;

public class KafkaOutboxMessageSender implements OutboxMessageSender {
    private final KafkaTemplate<Object, Object> kafka;
    private final ObjectMapper json;
    private final KafkaBusinessProperties topics;
    private final OutboxPublisherProperties properties;

    public KafkaOutboxMessageSender(
            KafkaTemplate<Object, Object> kafka,
            ObjectMapper json,
            KafkaBusinessProperties topics,
            OutboxPublisherProperties properties) {
        this.kafka = kafka;
        this.json = json;
        this.topics = topics;
        this.properties = properties;
    }

    @Override
    public void send(OutboxEventEntity event) throws Exception {
        var topic = topics.topicName(event.getDestination());
        var envelope = json.createObjectNode();
        envelope.put("eventId", event.getId().toString());
        envelope.put("aggregateType", event.getAggregateType());
        envelope.put("aggregateId", event.getAggregateId());
        envelope.put("eventType", event.getEventType());
        envelope.put("schemaVersion", event.getSchemaVersion());
        envelope.put("createdAt", event.getCreatedAt().toString());
        envelope.set("payload", json.readTree(event.getPayload()));
        kafka.send(topic, event.getAggregateId(), json.writeValueAsString(envelope))
                .get(properties.ackTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
}
