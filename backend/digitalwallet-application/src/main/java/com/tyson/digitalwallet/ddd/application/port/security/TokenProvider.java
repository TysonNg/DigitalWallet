package com.tyson.digitalwallet.ddd.application.port.security;

import com.tyson.digitalwallet.ddd.domain.model.entity.User;

import java.util.List;
import java.util.UUID;

public interface TokenProvider {
    String generateAccessToken(User user);
    String generateAccessToken(User user, String sessionId);
    long getAccessTokenExpiration();

    String generateRefreshToken(User user);
    long getRefreshTokenExpiration();

    UUID extractUserId(String token);
    String extractEmail(String token);
    String extractSessionId(String token);
    String extractRole(String token);
    List<String> extractRoles(String token);
    List<String> extractAuthorities(String token);
    long getRemainingExpiration(String token);
    boolean validateToken(String token);
}
