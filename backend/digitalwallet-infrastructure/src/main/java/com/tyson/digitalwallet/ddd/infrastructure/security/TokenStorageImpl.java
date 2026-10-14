package com.tyson.digitalwallet.ddd.infrastructure.security;

import com.tyson.digitalwallet.ddd.domain.service.security.TokenStorage;
import com.tyson.digitalwallet.ddd.infrastructure.cached.RedisInfrasService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class TokenStorageImpl implements TokenStorage {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String BLACKLIST_TOKEN_PREFIX = "blacklist_token:";
    private static final String ACTIVE_SESSION_PREFIX = "active_session:";

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final RedisInfrasService redisInfrasService;

    public TokenStorageImpl(RedisInfrasService redisInfrasService) {
        this.redisInfrasService = redisInfrasService;
    }

    @Override
    public void saveRefreshToken(String refreshToken, UUID userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisInfrasService.set(key, refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getRefreshToken(UUID userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        return redisInfrasService.get(key);
    }

    @Override
    public void deleteRefreshToken(UUID userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisInfrasService.delete(key);
    }

    @Override
    public void blacklistToken(String token, long remainingDuration) {
        String key = BLACKLIST_TOKEN_PREFIX + token;
        redisInfrasService.set(key, "revoked", remainingDuration, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isBlacklistToken(String token) {
        String key = BLACKLIST_TOKEN_PREFIX + token;
        return redisInfrasService.hasKey(key);
    }

    @Override
    public void saveActiveSession(UUID userId, String sessionId) {
        String key = ACTIVE_SESSION_PREFIX + userId;
        redisInfrasService.set(key, sessionId, refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getActiveSession(UUID userId) {
        String key = ACTIVE_SESSION_PREFIX + userId;
        return redisInfrasService.get(key);
    }

    @Override
    public void deleteActiveSession(UUID userId) {
        String key = ACTIVE_SESSION_PREFIX + userId;
        redisInfrasService.delete(key);
    }

    @Override
    public boolean isValidSession(UUID userId, String sessionId) {
        String currentSession = getActiveSession(userId);
        return currentSession != null && currentSession.equals(sessionId);
    }
}
