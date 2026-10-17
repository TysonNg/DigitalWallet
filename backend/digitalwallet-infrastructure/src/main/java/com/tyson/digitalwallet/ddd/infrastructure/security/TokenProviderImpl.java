package com.tyson.digitalwallet.ddd.infrastructure.security;

import com.tyson.digitalwallet.ddd.domain.model.entity.Authority;
import com.tyson.digitalwallet.ddd.domain.model.entity.Role;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.application.port.security.TokenProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TokenProviderImpl implements TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TokenProviderImpl.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(User user) {
        return generateAccessToken(user, UUID.randomUUID().toString());
    }

    @Override
    public String generateAccessToken(User user, String sessionId) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(accessTokenExpiration);

        Map<String, Object> claims = new HashMap<>();

        claims.put("email", user.getEmail());
        claims.put("phoneNumber", user.getPhoneNumber());

        List<String> roleNames;
        List<String> authorityNames = new ArrayList<>();
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            roleNames = user.getRoles().stream()
                    .map(Role::getName)
                    .toList();
            for (Role role : user.getRoles()) {
                if (role.getAuthorities() != null) {
                    for (Authority authority : role.getAuthorities()) {
                        if (!authorityNames.contains(authority.getName())) {
                            authorityNames.add(authority.getName());
                        }
                    }
                }
            }
        } else {
            roleNames = List.of(user.getRole() != null ? user.getRole().name() : "ROLE_USER");
        }
        claims.put("roles", roleNames);
        claims.put("role", roleNames.contains("ROLE_ADMIN") ? "ROLE_ADMIN" : roleNames.get(0));
        claims.put("authorities", authorityNames);

        claims.put("fullName", user.getFullName());
        if (sessionId != null) {
            claims.put("sessionId", sessionId);
        }

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer("digital-wallet")
                .subject(user.getId().toString())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    @Override
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(refreshTokenExpiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer("digital-wallet")
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    @Override
    public UUID extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String userId = claims.getSubject();
        return UUID.fromString(userId);
    }

    @Override
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email", String.class);
    }

    @Override
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        String singleRole = claims.get("role", String.class);
        if (singleRole != null && !singleRole.isBlank()) {
            return List.of(singleRole);
        }
        return List.of("ROLE_USER");
    }

    @Override
    public List<String> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        Object authObj = claims.get("authorities");
        if (authObj instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    @Override
    public String extractRole(String token) {
        List<String> roles = extractRoles(token);
        if (roles.contains("ROLE_ADMIN")) {
            return "ROLE_ADMIN";
        }
        return roles.isEmpty() ? "ROLE_USER" : roles.get(0);
    }

    @Override
    public String extractSessionId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("sessionId", String.class);
    }

    @Override
    public long getRemainingExpiration(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean validateToken(String token) {
        if(token == null || token.isBlank()) {
            log.warn("JWT token is empty or null");
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature or malformed token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
