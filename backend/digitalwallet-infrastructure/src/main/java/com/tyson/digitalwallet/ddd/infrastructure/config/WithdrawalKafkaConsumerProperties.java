package com.tyson.digitalwallet.ddd.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("spring.kafka.withdrawal-consumer")
public record WithdrawalKafkaConsumerProperties(
        @DefaultValue("1s") Duration retryInterval,
        @DefaultValue("2") long retries,
        @DefaultValue("12s") Duration dltAckTimeout) {
    public WithdrawalKafkaConsumerProperties {
        if (retryInterval == null
                || retryInterval.isNegative()
                || retryInterval.compareTo(Duration.ofSeconds(30)) > 0
                || retries < 0
                || retries > 10
                || dltAckTimeout == null
                || dltAckTimeout.toMillis() < 1
                || dltAckTimeout.compareTo(Duration.ofMinutes(1)) > 0)
            throw new IllegalArgumentException("Invalid withdrawal consumer retry/DLT settings");
    }
}
