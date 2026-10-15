package com.tyson.digitalwallet.ddd.infrastructure.security;

import com.tyson.digitalwallet.ddd.application.port.security.OtpStorage;
import com.tyson.digitalwallet.ddd.infrastructure.cached.RedisInfrasService;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OtpStorageImpl implements OtpStorage {

    private static final String OTP_PREFIX = "otp:register:";

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
}
