package com.tyson.digitalwallet.ddd.infrastructure.persistence.adapter;

import com.tyson.digitalwallet.ddd.domain.model.entity.Notification;
import com.tyson.digitalwallet.ddd.domain.service.sender.NotificationSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketNotificationSenderImpl implements NotificationSender {
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationSenderImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendNotification(Notification notification) {
        messagingTemplate.convertAndSendToUser(
                notification.getUserId().toString(),
                "/queue/notifications",
                notification
        );
    }
}
