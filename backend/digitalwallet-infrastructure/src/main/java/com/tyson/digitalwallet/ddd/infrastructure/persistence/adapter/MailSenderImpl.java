package com.tyson.digitalwallet.ddd.infrastructure.persistence.adapter;

import com.tyson.digitalwallet.ddd.domain.service.sender.MailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class MailSenderImpl implements MailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.email.username}")
    private String fromEmail;

    public MailSenderImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
        } catch (MailException e) {
            throw new RuntimeException( "Failed to send email " + to + "with error----" + e.getMessage());
        }

    }
}
