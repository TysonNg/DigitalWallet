package com.tyson.digitalwallet.ddd.application.usecase.notification.impl;

import com.tyson.digitalwallet.ddd.application.usecase.notification.NotificationUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.notification.response.NotificationResponse;
import com.tyson.digitalwallet.ddd.domain.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationUseCaseImpl implements NotificationUseCase {

    private final NotificationService notificationService;

    public NotificationUseCaseImpl(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(UUID userId) {
        return notificationService.getNotificationsByUserId(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadByUserId(UUID userId) {
        return notificationService.countUnreadByUserId(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationService.markAsRead(notificationId, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationService.markAllAsRead(userId);
    }
}
