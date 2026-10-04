package com.tyson.digitalwallet.ddd.application.usecase.auth.listener;

import com.tyson.digitalwallet.ddd.domain.event.UserRegisteredEvent;
import com.tyson.digitalwallet.ddd.domain.service.sender.MailSender;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserRegisteredEventListener {

    private final MailSender mailSender;

    public UserRegisteredEventListener(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        String subject = "Welcome to Digital Wallet!";
        Map<String, Object> variables = Map.of(
                "fullName", event.fullName() != null ? event.fullName() : "Valued Customer",
                "email", event.email()
        );

        mailSender.sendTemplate(
                event.email(),
                subject,
                "mail/welcome-email",
                variables
        );
    }
}
