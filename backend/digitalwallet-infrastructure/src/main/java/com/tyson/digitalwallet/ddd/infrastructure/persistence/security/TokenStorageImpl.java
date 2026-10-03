package com.tyson.digitalwallet.ddd.infrastructure.persistence.security;

import com.tyson.digitalwallet.ddd.domain.service.security.TokenStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class TokenStorageImpl implements TokenStorage {

    private final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private final String BLACKLIST_TOKEN_PREFIX = "blacklist_token:";

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final StringRedisTemplate redisTemplate;

    public TokenStorageImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveRefreshToken(String refreshToken, UUID userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;

        redisTemplate.opsForValue().set(key, refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getRefreshToken(UUID userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void deleteRefreshToken(UUID userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;

        redisTemplate.delete(key);
    }

    @Override
    public void blacklistToken(String token, long remainingDuration) {
        String key = BLACKLIST_TOKEN_PREFIX + token;

        redisTemplate.opsForValue().set(key, "revoked", remainingDuration, TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean isBlacklistToken(String token) {
        String key = BLACKLIST_TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
