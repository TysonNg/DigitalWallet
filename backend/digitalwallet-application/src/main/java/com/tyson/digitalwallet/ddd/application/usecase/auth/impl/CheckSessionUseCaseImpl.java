package com.tyson.digitalwallet.ddd.application.usecase.auth.impl;

import com.tyson.digitalwallet.ddd.application.exception.InvalidCredentialsException;
import com.tyson.digitalwallet.ddd.application.exception.SessionExpiredException;
import com.tyson.digitalwallet.ddd.application.usecase.auth.CheckSessionUseCase;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenProvider;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenStorage;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CheckSessionUseCaseImpl implements CheckSessionUseCase {

    private final TokenProvider tokenProvider;
    private final TokenStorage tokenStorage;

    public CheckSessionUseCaseImpl(TokenProvider tokenProvider, TokenStorage tokenStorage) {
        this.tokenProvider = tokenProvider;
        this.tokenStorage = tokenStorage;
    }

    @Override
    public void checkSession(String token) {
        if (token == null || !tokenProvider.validateToken(token)) {
            throw new InvalidCredentialsException("Invalid or expired token!");
        }

        // Kiểm tra xem token có nằm trong Blacklist không (ví dụ: đã đăng xuất)
        if (tokenStorage.isBlacklistToken(token)) {
            throw new SessionExpiredException("Token has been revoked. Please log in again.");
        }

        UUID userId = tokenProvider.extractUserId(token);
        String sessionIdFromToken = tokenProvider.extractSessionId(token);

        if (sessionIdFromToken == null) {
            throw new SessionExpiredException("Invalid session token. Please log in again.");
        }

        boolean isValid = tokenStorage.isValidSession(userId, sessionIdFromToken);
        if (!isValid) {
            throw new SessionExpiredException("Your account has been logged in on another device.");
        }
    }
}
