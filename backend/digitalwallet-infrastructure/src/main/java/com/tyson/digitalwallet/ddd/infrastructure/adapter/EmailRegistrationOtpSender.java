package com.tyson.digitalwallet.ddd.infrastructure.adapter;

import com.tyson.digitalwallet.ddd.application.usecase.auth.port.RegistrationOtpSender;
import com.tyson.digitalwallet.ddd.application.port.sender.MailSender;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;

@Component
public class EmailRegistrationOtpSender implements RegistrationOtpSender {
    private final MailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public EmailRegistrationOtpSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String generateOtp() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    }

    @Override
    public void send(String email, String fullName, String otp) {
        mailSender.sendTemplate(email, "Account Registration OTP - Digital Wallet", "mail/otp-email",
                Map.of("fullName", fullName != null ? fullName : "Valued Customer", "otpCode", otp));
    }
}