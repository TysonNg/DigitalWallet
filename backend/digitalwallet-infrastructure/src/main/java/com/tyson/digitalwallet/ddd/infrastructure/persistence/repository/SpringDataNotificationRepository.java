package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<NotificationEntity> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") UUID userId);
}
