package com.tyson.digitalwallet.ddd.domain.service.security;

public interface OtpStorage {
    void saveOtp(String email, String otp, long expirationMinutes);
    String getOtp(String email);
    boolean validateOtp(String email, String otp);
    void deleteOtp(String email);

    void savePendingRegistration(String email, String dataJson, long expirationMinutes);
    String getPendingRegistration(String email);
    void deletePendingRegistration(String email);
}
