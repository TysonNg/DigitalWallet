package com.tyson.digitalwallet.ddd.domain.service.sender;

import com.tyson.digitalwallet.ddd.domain.model.entity.Notification;

public interface NotificationSender {
    void sendNotification(Notification notification);
}
