package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Notification;
import com.tyson.digitalwallet.ddd.domain.repository.NotificationRepository;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.NotificationMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.NotificationEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {
    private SpringDataNotificationRepository notificationRepository;
    private NotificationMapper notificationMapper;

    public NotificationRepositoryImpl(
            NotificationMapper notificationMapper,
            SpringDataNotificationRepository notificationRepository
    ) {
        this.notificationMapper = notificationMapper;
        this.notificationRepository = notificationRepository;
    }


    @Override
    public Notification save(Notification notification) {
        NotificationEntity notificationEntity = notificationMapper.toEntity(notification);
        NotificationEntity saved = notificationRepository.save(notificationEntity);

        return notificationMapper.toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return notificationRepository.findById(id).map(notificationMapper::toDomain);
    }

    @Override
    public List<Notification> findByUserId(UUID userId) {
        return notificationRepository
                .findByUserId(userId)
                .stream()
                .map(notificationMapper::toDomain)
                .toList();
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Override
    public void markAllAsReadByUserId(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
