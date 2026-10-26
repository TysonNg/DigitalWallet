package com.tyson.digitalwallet.ddd.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Project-specific delivery settings, not built-in Kafka client properties. */
@ConfigurationProperties("spring.kafka.outbox")
public record OutboxPublisherProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("20") int batchSize,
        @DefaultValue("12s") Duration ackTimeout,
        @DefaultValue("1s") Duration initialBackoff,
        @DefaultValue("5m") Duration maxBackoff) {
    public OutboxPublisherProperties {
        if (batchSize < 1 || batchSize > 1000)
            throw new IllegalArgumentException("Outbox batch-size must be 1..1000");
        if (ackTimeout == null
                || ackTimeout.toMillis() < 1
                || ackTimeout.compareTo(Duration.ofMinutes(1)) > 0)
            throw new IllegalArgumentException("Outbox ack-timeout must be 1ms..1m");
        if (initialBackoff == null
                || maxBackoff == null
                || initialBackoff.toMillis() < 1
                || maxBackoff.compareTo(initialBackoff) < 0
                || maxBackoff.compareTo(Duration.ofDays(1)) > 0)
            throw new IllegalArgumentException(
                    "Outbox backoff must be positive, ordered, and at most 1 day");
    }

    public Duration retryDelay(int failures) {
        long delay = initialBackoff.toMillis(), cap = maxBackoff.toMillis();
        for (int i = 1; i < failures && delay < cap; i++) delay = delay > cap / 2 ? cap : delay * 2;
        return Duration.ofMillis(Math.min(delay, cap));
    }
}
