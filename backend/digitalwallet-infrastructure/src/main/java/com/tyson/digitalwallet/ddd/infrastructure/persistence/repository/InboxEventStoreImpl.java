package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.application.port.inbox.*;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.InboxEventMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.InboxEventEntity;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

import java.util.*;

@Repository
public class InboxEventStoreImpl implements InboxEventStore {
    private final SpringDataInboxEventRepository repository;
    private final InboxEventMapper mapper;

    public InboxEventStoreImpl(SpringDataInboxEventRepository repository, InboxEventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<InboxEventReceipt> find(String consumer, UUID id) {
        return repository.findById(new InboxEventEntity.Key(consumer, id)).map(mapper::toReceipt);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(InboxEventReceipt receipt) {
        repository.saveAndFlush(mapper.toEntity(receipt));
    }
}
