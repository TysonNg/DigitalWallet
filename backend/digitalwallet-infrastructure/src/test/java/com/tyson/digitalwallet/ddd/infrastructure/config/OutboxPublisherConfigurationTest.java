package com.tyson.digitalwallet.ddd.infrastructure.config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.infrastructure.messaging.outbox.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.repository.SpringDataOutboxEventRepository;

import org.junit.Test;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Duration;
import java.util.*;

public class OutboxPublisherConfigurationTest {
    @Configuration
    @EnableScheduling
    static class Scheduling {}

    @Test
    public void disabledPublisherNeedsNoDatabaseOrKafkaBeans() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(OutboxPublisherConfiguration.class);
            ctx.refresh();
            assertTrue(ctx.getBeansOfType(OutboxPublisher.class).isEmpty());
        }
    }

    @Test
    public void enabledConfigurationBindsSettingsAndSeparatesSchedulers() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment()
                    .getPropertySources()
                    .addFirst(
                            new MapPropertySource(
                                    "test",
                                    Map.of(
                                            "spring.kafka.outbox.enabled",
                                            "true",
                                            "spring.kafka.outbox.batch-size",
                                            "7",
                                            "spring.kafka.outbox.ack-timeout",
                                            "2s",
                                            "spring.kafka.outbox.initial-delay",
                                            "1h")));
            ctx.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            ctx.registerBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class));
            ctx.registerBean(
                    SpringDataOutboxEventRepository.class,
                    () -> mock(SpringDataOutboxEventRepository.class));
            ctx.registerBean(
                    PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class));
            ctx.register(
                    OutboxPublisherConfiguration.class,
                    KafkaTopicsConfiguration.class,
                    Scheduling.class);
            ctx.refresh();
            assertNotNull(ctx.getBean(OutboxPublisher.class));
            var properties = ctx.getBean(OutboxPublisherProperties.class);
            assertEquals(7, properties.batchSize());
            assertEquals(Duration.ofSeconds(2), properties.ackTimeout());
            var ordinary = ctx.getBean("taskScheduler", ThreadPoolTaskScheduler.class);
            var outbox = ctx.getBean("outboxTaskScheduler", ThreadPoolTaskScheduler.class);
            assertNotSame(ordinary, outbox);
            assertEquals(1, outbox.getPoolSize());
            assertEquals(
                    1,
                    ctx.getBean(ScheduledAnnotationBeanPostProcessor.class)
                            .getScheduledTasks()
                            .size());
        }
    }

    @Test
    public void validatesLimitsAndCapsBackoffWithoutOverflow() {
        var p =
                new OutboxPublisherProperties(
                        true,
                        20,
                        Duration.ofSeconds(12),
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(5));
        assertEquals(Duration.ofSeconds(1), p.retryDelay(1));
        assertEquals(Duration.ofSeconds(2), p.retryDelay(2));
        assertEquals(Duration.ofMinutes(5), p.retryDelay(Integer.MAX_VALUE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new OutboxPublisherProperties(
                                true,
                                0,
                                Duration.ofSeconds(12),
                                Duration.ofSeconds(1),
                                Duration.ofMinutes(5)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new OutboxPublisherProperties(
                                true,
                                20,
                                Duration.ZERO,
                                Duration.ofSeconds(1),
                                Duration.ofMinutes(5)));
    }
}
