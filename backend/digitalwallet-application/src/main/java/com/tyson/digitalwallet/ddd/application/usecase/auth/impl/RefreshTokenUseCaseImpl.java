package com.tyson.digitalwallet.ddd.application.usecase.auth.impl;

import com.tyson.digitalwallet.ddd.application.exception.InvalidCredentialsException;
import com.tyson.digitalwallet.ddd.application.exception.SessionExpiredException;
import com.tyson.digitalwallet.ddd.application.exception.UserNotActiveException;
import com.tyson.digitalwallet.ddd.application.usecase.auth.RefreshTokenUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.RefreshTokenCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenProvider;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenStorage;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final TokenStorage tokenStorage;

    public RefreshTokenUseCaseImpl(
            UserRepository userRepository,
            TokenProvider tokenProvider,
            TokenStorage tokenStorage
    ) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.tokenStorage = tokenStorage;
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenCommand command) {
        String inputRefreshToken = command.refreshToken();

        // 1. Kiểm tra tính hợp lệ của Refresh Token (chữ ký, thời hạn JWT)
        if (inputRefreshToken == null || !tokenProvider.validateToken(inputRefreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token!");
        }

        // 2. Trích xuất userId từ Refresh Token
        UUID userId = tokenProvider.extractUserId(inputRefreshToken);

        // 3. Đối chiếu Refresh Token với Redis xem có khớp phiên hiện tại không
        String storedRefreshToken = tokenStorage.getRefreshToken(userId);
        if (storedRefreshToken == null || !storedRefreshToken.equals(inputRefreshToken)) {
            throw new SessionExpiredException("Refresh token is invalid or expired. Please log in again.");
        }

        // 4. Kiểm tra sự tồn tại và trạng thái của người dùng
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found!"));

        if (!user.isActive()) {
            throw new UserNotActiveException("User account is " + user.getStatus().name().toLowerCase() + ". Please contact support.");
        }

        // 5. Lấy sessionId của phiên hiện tại (nếu chưa có thì sinh mới)
        String sessionId = tokenStorage.getActiveSession(userId);
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            tokenStorage.saveActiveSession(userId, sessionId);
        }

        // 6. Áp dụng cơ chế Refresh Token Rotation (RTR): Sinh cặp token mới
        String newAccessToken = tokenProvider.generateAccessToken(user, sessionId);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        // 7. Cập nhật Refresh Token mới vào Redis (hủy Refresh Token cũ)
        tokenStorage.saveRefreshToken(newRefreshToken, user.getId());

        // 8. Trả về cặp token mới cho Client
        return AuthResponse.of(
                newAccessToken,
                newRefreshToken,
                tokenProvider.getAccessTokenExpiration(),
                user
        );
    }
}
