package com.tyson.digitalwallet.ddd.application.usecase.notification;

import com.tyson.digitalwallet.ddd.application.usecase.notification.response.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationUseCase {
    List<NotificationResponse> getNotificationsByUserId(UUID userId);
    long countUnreadByUserId(UUID userId);
    void markAsRead(UUID notificationId, UUID userId);
    void markAllAsRead(UUID userId);
}
