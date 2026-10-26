package com.tyson.digitalwallet.ddd.infrastructure.scheduler;

import com.tyson.digitalwallet.ddd.infrastructure.messaging.outbox.OutboxPublisher;

import org.springframework.scheduling.annotation.Scheduled;

public class OutboxPublisherScheduler {
    private final OutboxPublisher publisher;

    public OutboxPublisherScheduler(OutboxPublisher publisher) {
        this.publisher = publisher;
    }

    @Scheduled(
            fixedDelayString = "${spring.kafka.outbox.poll-interval:1s}",
            initialDelayString = "${spring.kafka.outbox.initial-delay:5s}",
            scheduler = "outboxTaskScheduler")
    public void publish() {
        publisher.publishBatch();
    }
}
