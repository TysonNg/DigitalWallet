package com.tyson.digitalwallet.ddd.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MoneyTransferEvent(
        UUID transactionId,
        UUID senderUserId,
        UUID receiveUserId,
        BigDecimal amount,
        BigDecimal senderBalanceAfter,
        String description,
        Instant occurredAt
) implements DomainEvent {
}
