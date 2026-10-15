package com.tyson.digitalwallet.ddd.application.port.security;

public interface OtpStorage {
    void saveOtp(String email, String otp, long expirationMinutes);
    String getOtp(String email);
    boolean validateOtp(String email, String otp);
    void deleteOtp(String email);

}
