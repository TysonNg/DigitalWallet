package com.tyson.digitalwallet.ddd.application.usecase.notification.response;

import com.tyson.digitalwallet.ddd.domain.model.entity.Notification;
import com.tyson.digitalwallet.ddd.domain.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String title,
        String content,
        UUID transactionId,
        NotificationType type,
        boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTransactionId(),
                notification.getType(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
