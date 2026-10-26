package com.tyson.digitalwallet.ddd.infrastructure.messaging.withdrawal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.exception.InvalidWithdrawalEventException;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.ConsumeWithdrawalEventUseCase;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;

import java.util.UUID;

public class WithdrawalProcessKafkaListenerTest {
    private final ObjectMapper json = new ObjectMapper();
    private final ConsumeWithdrawalEventUseCase consumer =
            mock(ConsumeWithdrawalEventUseCase.class);
    private final UUID id = UUID.randomUUID(), eventId = UUID.randomUUID();

    private String body() {
        return "{\"eventId\":\""
                + eventId
                + "\",\"aggregateType\":\"WITHDRAWAL\",\"aggregateId\":\""
                + id
                + "\",\"eventType\":\"WITHDRAWAL_PROCESS_REQUESTED\",\"schemaVersion\":1,\"createdAt\":\"2026-10-26T00:00:00Z\",\"payload\":{\"withdrawalId\":\""
                + id
                + "\"}}";
    }

    private void receive(String key, String body) {
        new WithdrawalProcessKafkaListener(json, consumer)
                .receive(new ConsumerRecord<>("withdrawal", 0, 0, key, body));
    }

    @Test
    public void validEnvelopeInvokesApplication() {
        receive(id.toString(), body());
        verify(consumer).consume(eventId, id);
    }

    @Test
    public void rejectsInvalidJsonVersionTypesAndMismatchedIdentity() {
        for (var raw :
                java.util.List.of(
                        "bad json",
                        "null",
                        body().replace("WITHDRAWAL_PROCESS_REQUESTED", "OTHER"),
                        body().replace("\"schemaVersion\":1", "\"schemaVersion\":2"),
                        body().replace("\"schemaVersion\":1", "\"schemaVersion\":1.5"),
                        body().replace("\"WITHDRAWAL\"", "\"PAYMENT\""),
                        body().replace(eventId.toString(), "1-1-1-1-1"),
                        body().replace("2026-10-26T00:00:00Z", "bad timestamp"),
                        body().replace(
                                        "\"withdrawalId\":\"" + id,
                                        "\"withdrawalId\":\"" + UUID.randomUUID())))
            assertThrows(InvalidWithdrawalEventException.class, () -> receive(id.toString(), raw));
        assertThrows(
                InvalidWithdrawalEventException.class,
                () -> receive(UUID.randomUUID().toString(), body()));
        assertThrows(InvalidWithdrawalEventException.class, () -> receive(id.toString(), null));
        verifyNoInteractions(consumer);
    }

    @Test
    public void temporaryApplicationErrorsPropagateForRetry() {
        doThrow(new IllegalStateException()).when(consumer).consume(eventId, id);
        assertThrows(IllegalStateException.class, () -> receive(id.toString(), body()));
    }
}
