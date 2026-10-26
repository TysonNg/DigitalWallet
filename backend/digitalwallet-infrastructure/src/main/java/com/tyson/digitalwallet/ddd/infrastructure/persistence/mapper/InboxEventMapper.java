package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.application.port.inbox.InboxEventReceipt;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.InboxEventEntity;

import org.springframework.stereotype.Component;

@Component
public class InboxEventMapper {
    public InboxEventEntity toEntity(InboxEventReceipt receipt) {
        var e = new InboxEventEntity();
        e.setConsumerName(receipt.consumerName());
        e.setEventId(receipt.eventId());
        e.setAggregateId(receipt.aggregateId());
        e.setProcessedAt(receipt.processedAt());
        return e;
    }

    public InboxEventReceipt toReceipt(InboxEventEntity e) {
        return new InboxEventReceipt(
                e.getConsumerName(), e.getEventId(), e.getAggregateId(), e.getProcessedAt());
    }
}
