package com.tyson.digitalwallet.ddd.application.port.inbox;

import java.util.*;

public interface InboxEventStore {
    Optional<InboxEventReceipt> find(String consumerName, UUID eventId);

    void save(InboxEventReceipt receipt);
}
