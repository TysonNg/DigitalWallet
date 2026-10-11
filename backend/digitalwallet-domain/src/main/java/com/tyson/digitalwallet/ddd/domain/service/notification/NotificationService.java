package com.tyson.digitalwallet.ddd.domain.service.notification;

import com.tyson.digitalwallet.ddd.domain.model.entity.Notification;
import com.tyson.digitalwallet.ddd.domain.model.enums.NotificationType;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    Notification createNotification(UUID userId, String title, String content, UUID transactionId, NotificationType type);

    void markAsRead(UUID notificationId, UUID userId);

    void markAllAsRead(UUID userId);

    List<Notification> getNotificationsByUserId(UUID userId);

    long countUnreadByUserId(UUID userId);
}
