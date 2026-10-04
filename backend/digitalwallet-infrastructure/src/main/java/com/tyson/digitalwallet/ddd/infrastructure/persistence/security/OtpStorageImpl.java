package com.tyson.digitalwallet.ddd.infrastructure.persistence.security;

import com.tyson.digitalwallet.ddd.domain.service.security.OtpStorage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OtpStorageImpl implements OtpStorage {

    private static final String OTP_PREFIX = "otp:register:";
    private static final String PENDING_PREFIX = "pending:register:";

    private final StringRedisTemplate redisTemplate;

    public OtpStorageImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveOtp(String email, String otp, long expirationMinutes) {
        String key = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(key, otp, expirationMinutes, TimeUnit.MINUTES);
    }

    @Override
    public String getOtp(String email) {
        String key = OTP_PREFIX + email;
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean validateOtp(String email, String otp) {
        String storedOtp = getOtp(email);
        return storedOtp != null && storedOtp.equals(otp);
    }

    @Override
    public void deleteOtp(String email) {
        String key = OTP_PREFIX + email;
        redisTemplate.delete(key);
    }

    @Override
    public void savePendingRegistration(String email, String dataJson, long expirationMinutes) {
        String key = PENDING_PREFIX + email;
        redisTemplate.opsForValue().set(key, dataJson, expirationMinutes, TimeUnit.MINUTES);
    }

    @Override
    public String getPendingRegistration(String email) {
        String key = PENDING_PREFIX + email;
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void deletePendingRegistration(String email) {
        String key = PENDING_PREFIX + email;
        redisTemplate.delete(key);
    }
}
