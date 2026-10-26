package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.application.port.outbox.OutboxEvent;
import com.tyson.digitalwallet.ddd.application.port.outbox.OutboxEventWriter;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.OutboxEventMapper;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OutboxEventWriterImpl implements OutboxEventWriter {
    private final SpringDataOutboxEventRepository repository;
    private final OutboxEventMapper mapper;

    public OutboxEventWriterImpl(
            SpringDataOutboxEventRepository repository, OutboxEventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(OutboxEvent event) {
        // Flush in the caller's transaction so constraint/serialization failures abort it.
        repository.saveAndFlush(mapper.toEntity(event));
    }
}
