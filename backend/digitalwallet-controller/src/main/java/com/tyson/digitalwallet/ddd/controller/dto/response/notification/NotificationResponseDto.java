package com.tyson.digitalwallet.ddd.controller.dto.response.notification;

import com.tyson.digitalwallet.ddd.application.usecase.notification.response.NotificationResponse;
import com.tyson.digitalwallet.ddd.domain.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponseDto(
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
    public static NotificationResponseDto from(NotificationResponse response) {
        if (response == null) {
            return null;
        }
        return new NotificationResponseDto(
                response.id(),
                response.userId(),
                response.title(),
                response.content(),
                response.transactionId(),
                response.type(),
                response.isRead(),
                response.readAt(),
                response.createdAt()
        );
    }
}
