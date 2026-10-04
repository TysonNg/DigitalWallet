package com.tyson.digitalwallet.ddd.infrastructure.persistence.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenProvider;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenStorage;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final TokenStorage tokenStorage;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            TokenProvider tokenProvider,
            TokenStorage tokenStorage,
            ObjectMapper objectMapper
    ) {
        this.tokenProvider = tokenProvider;
        this.tokenStorage = tokenStorage;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/login") ||
               path.startsWith("/api/v1/auth/register") ||
               path.startsWith("/api/v1/auth/refresh-token") ||
               path.startsWith("/auth/login") ||
               path.startsWith("/auth/register") ||
               path.startsWith("/auth/refresh-token");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Trích xuất Authorization Header
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Không có token -> Cho đi tiếp để Spring Security xử lý bằng AuthenticationEntryPoint nếu endpoint yêu cầu bảo vệ
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // 3. Kiểm tra tính hợp lệ và thời hạn của Access Token
        if (!tokenProvider.validateToken(token)) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid or expired access token!");
            return;
        }

        // 4. Kiểm tra xem Token có bị Blacklist (đã logout) không
        if (tokenStorage.isBlacklistToken(token)) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token has been revoked. Please log in again.");
            return;
        }

        // 5. Kiểm tra Session có bị đăng nhập đè từ thiết bị khác không
        UUID userId = tokenProvider.extractUserId(token);
        String sessionId = tokenProvider.extractSessionId(token);

        if (sessionId == null || !tokenStorage.isValidSession(userId, sessionId)) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Your account has been logged in on another device.");
            return;
        }

        // 6. Token hoàn toàn hợp lệ -> Nạp Authentication vào SecurityContext
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("success", false);
        errorBody.put("statusCode", status.value());
        errorBody.put("message", message);
        errorBody.put("data", null);
        errorBody.put("timestamp", Instant.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
