package com.tyson.digitalwallet.ddd.application.usecase.notification.impl;

import com.tyson.digitalwallet.ddd.application.usecase.notification.NotificationUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.notification.response.NotificationResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.Notification;
import com.tyson.digitalwallet.ddd.domain.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationUseCaseImpl implements NotificationUseCase {

    private final NotificationRepository notificationRepository;

    public NotificationUseCaseImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadByUserId(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));

        // Kiểm tra quyền sở hữu thông báo
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You are not allowed to update this notification");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
