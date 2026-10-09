package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(UUID id);
    List<Notification> findByUserId(UUID userId);
    long countUnreadByUserId(UUID userId);
    void markAllAsReadByUserId(UUID userId);
}
