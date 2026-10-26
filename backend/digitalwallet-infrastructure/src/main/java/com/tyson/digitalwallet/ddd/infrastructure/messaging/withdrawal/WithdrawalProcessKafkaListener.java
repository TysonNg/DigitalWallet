package com.tyson.digitalwallet.ddd.infrastructure.messaging.withdrawal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.tyson.digitalwallet.ddd.application.exception.InvalidWithdrawalEventException;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.ConsumeWithdrawalEventUseCase;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.Instant;
import java.util.UUID;

public class WithdrawalProcessKafkaListener {
    private final ObjectMapper json;
    private final ConsumeWithdrawalEventUseCase consumer;

    public WithdrawalProcessKafkaListener(
            ObjectMapper json, ConsumeWithdrawalEventUseCase consumer) {
        this.json = json;
        this.consumer = consumer;
    }

    @KafkaListener(
            id = "withdrawal-process",
            topics = "${spring.kafka.topics.withdrawal-process.name}",
            groupId = "${spring.kafka.groups.withdrawal-worker}",
            containerFactory = "withdrawalKafkaListenerContainerFactory")
    public void receive(ConsumerRecord<String, String> record) {
        final UUID eventId, withdrawalId;
        try {
            if (record.value() == null) throw new IllegalArgumentException();
            var envelope = json.readTree(record.value());
            if (envelope == null
                    || !envelope.isObject()
                    || !"WITHDRAWAL".equals(text(envelope, "aggregateType"))
                    || !"WITHDRAWAL_PROCESS_REQUESTED".equals(text(envelope, "eventType"))
                    || !envelope.path("schemaVersion").isIntegralNumber()
                    || !envelope.path("schemaVersion").canConvertToInt()
                    || envelope.path("schemaVersion").intValue() != 1
                    || !envelope.path("payload").isObject()) throw new IllegalArgumentException();
            eventId = uuid(text(envelope, "eventId"));
            withdrawalId = uuid(text(envelope.path("payload"), "withdrawalId"));
            if (!withdrawalId.equals(uuid(text(envelope, "aggregateId")))
                    || !withdrawalId.equals(uuid(record.key())))
                throw new IllegalArgumentException();
            Instant.parse(text(envelope, "createdAt"));
        } catch (JsonProcessingException
                | IllegalArgumentException
                | java.time.format.DateTimeParseException ex) {
            throw new InvalidWithdrawalEventException(
                    "Invalid withdrawal event envelope or unsupported version");
        }
        // Business/database/provider exceptions must escape so the container does not commit yet.
        consumer.consume(eventId, withdrawalId);
    }

    private static String text(JsonNode node, String field) {
        var value = node.path(field);
        if (!value.isTextual()) throw new IllegalArgumentException();
        return value.textValue();
    }

    private static UUID uuid(String raw) {
        if (raw == null) throw new IllegalArgumentException();
        var id = UUID.fromString(raw);
        if (!id.toString().equalsIgnoreCase(raw)) throw new IllegalArgumentException();
        return id;
    }
}
