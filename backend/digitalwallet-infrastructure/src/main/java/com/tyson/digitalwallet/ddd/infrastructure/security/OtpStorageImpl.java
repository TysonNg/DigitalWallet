package com.tyson.digitalwallet.ddd.infrastructure.security;

import com.tyson.digitalwallet.ddd.domain.service.security.OtpStorage;
import com.tyson.digitalwallet.ddd.infrastructure.cached.RedisInfrasService;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OtpStorageImpl implements OtpStorage {

    private static final String OTP_PREFIX = "otp:register:";
    private static final String PENDING_PREFIX = "pending:register:";

    private final RedisInfrasService redisInfrasService;

    public OtpStorageImpl(RedisInfrasService redisInfrasService) {
        this.redisInfrasService = redisInfrasService;
    }

    @Override
    public void saveOtp(String email, String otp, long expirationMinutes) {
        String key = OTP_PREFIX + email;
        redisInfrasService.set(key, otp, expirationMinutes, TimeUnit.MINUTES);
    }

    @Override
    public String getOtp(String email) {
        String key = OTP_PREFIX + email;
        return redisInfrasService.get(key);
    }

    @Override
    public boolean validateOtp(String email, String otp) {
        String storedOtp = getOtp(email);
        return storedOtp != null && storedOtp.equals(otp);
    }

    @Override
    public void deleteOtp(String email) {
        String key = OTP_PREFIX + email;
        redisInfrasService.delete(key);
    }

    @Override
    public void savePendingRegistration(String email, String dataJson, long expirationMinutes) {
        String key = PENDING_PREFIX + email;
        redisInfrasService.set(key, dataJson, expirationMinutes, TimeUnit.MINUTES);
    }

    @Override
    public String getPendingRegistration(String email) {
        String key = PENDING_PREFIX + email;
        return redisInfrasService.get(key);
    }

    @Override
    public void deletePendingRegistration(String email) {
        String key = PENDING_PREFIX + email;
        redisInfrasService.delete(key);
    }
}
