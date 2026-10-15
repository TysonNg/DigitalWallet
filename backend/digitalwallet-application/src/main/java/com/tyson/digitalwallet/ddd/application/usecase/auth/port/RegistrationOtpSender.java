package com.tyson.digitalwallet.ddd.application.usecase.auth.port;

public interface RegistrationOtpSender {
    String generateOtp();
    void send(String email, String fullName, String otp);
}