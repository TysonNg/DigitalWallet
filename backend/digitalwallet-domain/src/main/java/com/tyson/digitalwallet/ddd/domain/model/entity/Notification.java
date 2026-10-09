package com.tyson.digitalwallet.ddd.domain.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public class Notification {
    private UUID id;
    private UUID userId;
    private String title;
    private String content;
    private UUID transactionId;
    private NotificationType type;
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;


    public static Notification create(
            UUID userId,
            String title,
            String content,
            UUID transactionId,
            NotificationType type
    ) {
        return new Notification(
                UUID.randomUUID(),
                userId,
                title,
                content,
                transactionId,
                type,
                false,
                null,
                LocalDateTime.now()
        );
    }

    public void markAsRead() {
        if(!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    public Notification() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Notification(UUID id, UUID userId, String title, String content, UUID transactionId, NotificationType type, boolean isRead, LocalDateTime readAt, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.transactionId = transactionId;
        this.type = type;
        this.isRead = isRead;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }
}
