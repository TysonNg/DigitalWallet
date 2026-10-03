package com.tyson.digitalwallet.ddd.domain.service.sender;

public interface MailSender {
    void send(String to, String subject, String content);
}
