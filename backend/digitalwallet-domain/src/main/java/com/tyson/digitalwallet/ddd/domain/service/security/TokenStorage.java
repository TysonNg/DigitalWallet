package com.tyson.digitalwallet.ddd.domain.service.security;

import java.util.UUID;

public interface TokenStorage {
    void saveRefreshToken(String refreshToken, UUID userId);
    String getRefreshToken(UUID userId);
    void deleteRefreshToken(UUID userId);

    void blacklistToken(String token, long remainingDuration);
    boolean isBlacklistToken(String token);
}
