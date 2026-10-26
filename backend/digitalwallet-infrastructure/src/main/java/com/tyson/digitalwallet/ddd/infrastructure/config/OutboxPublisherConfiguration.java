package com.tyson.digitalwallet.ddd.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.infrastructure.messaging.outbox.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.repository.SpringDataOutboxEventRepository;
import com.tyson.digitalwallet.ddd.infrastructure.scheduler.OutboxPublisherScheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.kafka.outbox", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OutboxPublisherProperties.class)
public class OutboxPublisherConfiguration {
    @Bean
    public OutboxMessageSender outboxMessageSender(
            KafkaTemplate<Object, Object> kafka,
            ObjectMapper json,
            KafkaBusinessProperties topics,
            OutboxPublisherProperties settings) {
        return new KafkaOutboxMessageSender(kafka, json, topics, settings);
    }

    @Bean
    public OutboxPublisher outboxPublisher(
            SpringDataOutboxEventRepository repository,
            OutboxMessageSender sender,
            OutboxPublisherProperties settings,
            PlatformTransactionManager manager) {
        return new OutboxPublisher(repository, sender, settings, manager, Clock.systemUTC());
    }

    @Bean
    public OutboxPublisherScheduler outboxPublisherScheduler(OutboxPublisher publisher) {
        return new OutboxPublisherScheduler(publisher);
    }

    // Preserve a default scheduler for existing unqualified @Scheduled jobs.
    @Bean(name = "taskScheduler")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(
            name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("scheduled-");
        return scheduler;
    }

    @Bean
    public ThreadPoolTaskScheduler outboxTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("outbox-publisher-");
        return scheduler;
    }
}
