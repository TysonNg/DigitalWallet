package com.tyson.digitalwallet.ddd.infrastructure.messaging.outbox;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.infrastructure.config.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.OutboxEventEntity;

import org.junit.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class KafkaOutboxMessageSenderTest {
    @SuppressWarnings("unchecked")
    @Test
    public void sendsEnvelopeWithRawPayloadAndWaitsForAck() throws Exception {
        var template = (KafkaTemplate<Object, Object>) mock(KafkaTemplate.class);
        var json = new ObjectMapper();
        var event = event();
        var pending = new CompletableFuture<SendResult<Object, Object>>();
        when(template.send(eq("events.payment.v1"), eq("p1"), any())).thenReturn(pending);
        var sender = new KafkaOutboxMessageSender(template, json, topics(), settings());
        var pool = Executors.newSingleThreadExecutor();
        try {
            var running =
                    pool.submit(
                            () -> {
                                sender.send(event);
                                return null;
                            });
            var body = org.mockito.ArgumentCaptor.forClass(Object.class);
            verify(template, timeout(2000)).send(eq("events.payment.v1"), eq("p1"), body.capture());
            assertFalse(running.isDone());
            var tree = json.readTree((String) body.getValue());
            assertEquals(event.getId().toString(), tree.path("eventId").asText());
            assertEquals("PAYMENT_COMPLETED", tree.path("eventType").asText());
            assertEquals(1, tree.path("schemaVersion").asInt());
            assertEquals("p1", tree.path("payload").path("paymentId").asText());
            assertTrue(tree.path("payload").isObject());
            pending.complete(null);
            running.get(2, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void failureAndUnknownDestinationAreNotAcknowledged() throws Exception {
        var template = (KafkaTemplate<Object, Object>) mock(KafkaTemplate.class);
        when(template.send(anyString(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("offline")));
        var sender =
                new KafkaOutboxMessageSender(template, new ObjectMapper(), topics(), settings());
        assertThrows(ExecutionException.class, () -> sender.send(event()));
        clearInvocations(template);
        var event = event();
        event.setDestination("unknown");
        assertThrows(IllegalArgumentException.class, () -> sender.send(event));
        verifyNoInteractions(template);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void neverAcknowledgedSendTimesOut() {
        var template = (KafkaTemplate<Object, Object>) mock(KafkaTemplate.class);
        when(template.send(anyString(), any(), any())).thenReturn(new CompletableFuture<>());
        var properties =
                new OutboxPublisherProperties(
                        true,
                        2,
                        Duration.ofMillis(25),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(8));
        var sender =
                new KafkaOutboxMessageSender(template, new ObjectMapper(), topics(), properties);
        assertThrows(TimeoutException.class, () -> sender.send(event()));
    }

    private static KafkaBusinessProperties topics() {
        return new KafkaBusinessProperties(
                null,
                Map.of(
                        "payment",
                        new KafkaBusinessProperties.TopicDefinition(
                                "events.payment.v1", 1, (short) 1)),
                null);
    }

    private static OutboxPublisherProperties settings() {
        return new OutboxPublisherProperties(
                true, 2, Duration.ofSeconds(3), Duration.ofSeconds(1), Duration.ofSeconds(8));
    }

    private static OutboxEventEntity event() {
        var e = new OutboxEventEntity();
        e.setId(UUID.randomUUID());
        e.setAggregateType("PAYMENT");
        e.setAggregateId("p1");
        e.setEventType("PAYMENT_COMPLETED");
        e.setSchemaVersion(1);
        e.setDestination("payment");
        e.setPayload("{\"paymentId\":\"p1\"}");
        e.setCreatedAt(Instant.parse("2026-10-26T00:00:00Z"));
        return e;
    }
}
