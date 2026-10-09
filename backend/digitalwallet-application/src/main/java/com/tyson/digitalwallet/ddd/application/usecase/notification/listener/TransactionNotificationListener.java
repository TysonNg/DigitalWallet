package com.tyson.digitalwallet.ddd.application.usecase.notification.listener;

import com.tyson.digitalwallet.ddd.domain.event.MoneyDepositEvent;
import com.tyson.digitalwallet.ddd.domain.event.MoneyTransferEvent;
import com.tyson.digitalwallet.ddd.domain.event.MoneyWithdrawnEvent;
import com.tyson.digitalwallet.ddd.domain.model.entity.Notification;
import com.tyson.digitalwallet.ddd.domain.model.enums.NotificationType;
import com.tyson.digitalwallet.ddd.domain.repository.NotificationRepository;
import com.tyson.digitalwallet.ddd.domain.service.sender.NotificationSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransactionNotificationListener {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    public TransactionNotificationListener(NotificationRepository notificationRepository, NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
        this.notificationRepository = notificationRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMoneyTransfer(MoneyTransferEvent event) {
        // Notification for sender
        Notification senderNotification = Notification.create(
                event.senderUserId(),
                "Transfer Successful",
                String.format("You have successfully transferred %,.0f VND. New balance: %,.0f VND.",
                        event.amount(), event.senderBalanceAfter()),
                event.transactionId(),
                NotificationType.TRANSFER
        );
        Notification savedSender = notificationRepository.save(senderNotification);
        notificationSender.sendNotification(savedSender);

        // Notification for receiver
        String description = (event.description() != null && !event.description().isBlank())
                ? event.description()
                : "Money Transfer";
        Notification receiverNotification = Notification.create(
                event.receiveUserId(),
                "Money Received",
                String.format("You received %,.0f VND. Description: %s",
                        event.amount(), description),
                event.transactionId(),
                NotificationType.TRANSFER
        );
        Notification savedReceiver = notificationRepository.save(receiverNotification);
        notificationSender.sendNotification(savedReceiver);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMoneyDeposit(MoneyDepositEvent event) {
        Notification notification = Notification.create(
                event.userId(),
                "Deposit Successful",
                String.format("You have successfully deposited %,.0f VND into your wallet. New balance: %,.0f VND.",
                        event.amount(), event.balanceAfter()),
                event.transactionId(),
                NotificationType.DEPOSIT
        );
        Notification saved = notificationRepository.save(notification);
        notificationSender.sendNotification(saved);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMoneyWithdrawn(MoneyWithdrawnEvent event) {
        Notification notification = Notification.create(
                event.userId(),
                "Withdrawal Successful",
                String.format("You have successfully withdrawn %,.0f VND from your wallet. New balance: %,.0f VND.",
                        event.amount(), event.balanceAfter()),
                event.transactionId(),
                NotificationType.WITHDRAW
        );
        Notification saved = notificationRepository.save(notification);
        notificationSender.sendNotification(saved);
    }
}
