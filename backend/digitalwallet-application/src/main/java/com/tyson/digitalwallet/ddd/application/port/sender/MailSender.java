package com.tyson.digitalwallet.ddd.application.port.sender;

import java.util.Map;

public interface MailSender {
    void send(String to, String subject, String content);
    void sendTemplate(String to, String subject, String templateName, Map<String, Object> variables);
}
