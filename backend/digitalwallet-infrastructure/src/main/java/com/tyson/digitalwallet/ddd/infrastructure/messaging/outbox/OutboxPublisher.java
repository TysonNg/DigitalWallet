package com.tyson.digitalwallet.ddd.infrastructure.messaging.outbox;

import com.tyson.digitalwallet.ddd.infrastructure.config.OutboxPublisherProperties;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.repository.SpringDataOutboxEventRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final SpringDataOutboxEventRepository repository;
    private final OutboxMessageSender sender;
    private final OutboxPublisherProperties properties;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public OutboxPublisher(
            SpringDataOutboxEventRepository repository,
            OutboxMessageSender sender,
            OutboxPublisherProperties properties,
            PlatformTransactionManager manager,
            Clock clock) {
        this.repository = repository;
        this.sender = sender;
        this.properties = properties;
        this.clock = clock;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Each iteration locks only one outbox row; no business transaction is held during sends. */
    public int publishBatch() {
        int processed = 0;
        while (processed < properties.batchSize() && !Thread.currentThread().isInterrupted()) {
            Boolean found =
                    transaction.execute(
                            status -> {
                                var next = repository.lockNextAvailable(clock.instant());
                                if (next.isEmpty()) return false;
                                var event = next.get();
                                try {
                                    sender.send(event);
                                    event.setPublishedAt(clock.instant());
                                    event.setLastError(null);
                                } catch (Exception ex) {
                                    if (ex instanceof InterruptedException)
                                        Thread.currentThread().interrupt();
                                    int failures =
                                            event.getAttempts() == Integer.MAX_VALUE
                                                    ? Integer.MAX_VALUE
                                                    : event.getAttempts() + 1;
                                    event.setAttempts(failures);
                                    event.setAvailableAt(
                                            clock.instant().plus(properties.retryDelay(failures)));
                                    // Store no exception message: transport errors can contain
                                    // application data.
                                    event.setLastError(ex.getClass().getSimpleName());
                                    log.warn(
                                            "Outbox delivery failed: eventId={}, errorType={}",
                                            event.getId(),
                                            event.getLastError());
                                }
                                // Dirty checking updates this managed entity at commit, while its
                                // row remains locked.
                                // Commit failures propagate; an acknowledged event can then be
                                // delivered again.
                                return true;
                            });
            if (!Boolean.TRUE.equals(found)) break;
            processed++;
        }
        return processed;
    }
}
