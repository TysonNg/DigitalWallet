package com.tyson.digitalwallet.ddd.application.port.payment;

import java.util.*;

public interface PayoutNotificationInbox {
    void enqueue(String provider, String eventId, UUID withdrawalId);

    List<Notification> pending();

    void complete(String provider, String eventId);

    record Notification(String provider, String eventId, UUID withdrawalId) {}
}
