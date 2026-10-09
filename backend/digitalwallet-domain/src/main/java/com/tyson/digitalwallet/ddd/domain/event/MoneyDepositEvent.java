package com.tyson.digitalwallet.ddd.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MoneyDepositEvent(
        UUID transactionId,
        UUID userId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant occurredAt
)
implements DomainEvent{
}
