package com.tyson.digitalwallet.ddd.infrastructure.adapter;

import com.tyson.digitalwallet.ddd.domain.exception.ServiceUnavailableException;
import com.tyson.digitalwallet.ddd.domain.service.sender.MailSender;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class MailSenderImpl implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(MailSenderImpl.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public MailSenderImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    @CircuitBreaker(name = "mailSender", fallbackMethod = "sendFallback")
    public void send(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send simple email to [{}]: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email to " + to + " with error: " + e.getMessage(), e);
        }
    }

    public void sendFallback(String to, String subject, String content, Throwable t) {
        log.error("MailSender circuit breaker fallback triggered for [{}] - subject [{}]: {}", to, subject, t.getMessage());
        throw new ServiceUnavailableException("Hệ thống gửi email thông báo tạm thời gián đoạn. Vui lòng thử lại sau ít phút.", t);
    }

    @Override
    @CircuitBreaker(name = "mailSender", fallbackMethod = "sendTemplateFallback")
    public void sendTemplate(String to, String subject, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        String htmlContent = templateEngine.process(templateName, context);
        sendHtml(to, subject, htmlContent);
    }

    public void sendTemplateFallback(String to, String subject, String templateName, Map<String, Object> variables, Throwable t) {
        log.error("MailSender template circuit breaker fallback triggered for [{}] - template [{}]: {}", to, templateName, t.getMessage());
        throw new ServiceUnavailableException("Hệ thống gửi email xác thực tạm thời gián đoạn. Vui lòng thử lại sau ít phút.", t);
    }

    private void sendHtml(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = kích hoạt chế độ HTML
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to [{}]: {}", to, e.getMessage());
            throw new RuntimeException("Gửi email thất bại tới: " + to + " - Lỗi: " + e.getMessage(), e);
        }
    }
}
