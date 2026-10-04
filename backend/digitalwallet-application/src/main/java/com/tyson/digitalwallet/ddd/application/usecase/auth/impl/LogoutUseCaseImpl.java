package com.tyson.digitalwallet.ddd.application.usecase.auth.impl;

import com.tyson.digitalwallet.ddd.application.exception.InvalidCredentialsException;
import com.tyson.digitalwallet.ddd.application.usecase.auth.LogoutUseCase;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenProvider;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenStorage;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final TokenProvider tokenProvider;
    private final TokenStorage tokenStorage;

    public LogoutUseCaseImpl(TokenProvider tokenProvider, TokenStorage tokenStorage) {
        this.tokenProvider = tokenProvider;
        this.tokenStorage = tokenStorage;
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new InvalidCredentialsException("Access token is missing!");
        }

        // 1. Kiểm tra token có hợp lệ không
        if (!tokenProvider.validateToken(accessToken)) {
            throw new InvalidCredentialsException("Invalid or expired token!");
        }

        UUID userId = tokenProvider.extractUserId(accessToken);
        long remainingDuration = tokenProvider.getRemainingExpiration(accessToken);

        // 2. Đưa Access Token vào Blacklist với thời gian hết hạn còn lại
        if (remainingDuration > 0) {
            tokenStorage.blacklistToken(accessToken, remainingDuration);
        }

        // 3. Xóa Active Session và Refresh Token của người dùng khỏi Redis
        tokenStorage.deleteRefreshToken(userId);
        tokenStorage.deleteActiveSession(userId);
    }
}
