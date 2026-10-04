package com.tyson.digitalwallet.ddd.application.usecase.auth.impl;

import com.tyson.digitalwallet.ddd.application.exception.InvalidCredentialsException;
import com.tyson.digitalwallet.ddd.application.exception.UserNotActiveException;
import com.tyson.digitalwallet.ddd.application.usecase.auth.LoginUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.LoginCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;
import com.tyson.digitalwallet.ddd.domain.event.UserLoggedInEvent;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.service.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenProvider;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenStorage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final TokenStorage tokenStorage;
    private final ApplicationEventPublisher eventPublisher;

    public LoginUseCaseImpl(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            TokenStorage tokenStorage,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.tokenStorage = tokenStorage;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AuthResponse login(LoginCommand loginCommand) {
        // 1. Kiểm tra sự tồn tại của người dùng theo email
        User user = userRepository.findByEmail(loginCommand.email())
                .orElseThrow(InvalidCredentialsException::new);

        // 2. Xác thực mật khẩu
        boolean isPasswordValid = passwordHasher.matches(loginCommand.password(), user.getPassword());
        if (!isPasswordValid) {
            throw new InvalidCredentialsException();
        }

        // 3. Kiểm tra trạng thái tài khoản
        if (!user.isActive()) {
            throw new UserNotActiveException("User account is " + user.getStatus().name().toLowerCase() + ". Please contact support.");
        }

        // 4. Sinh mã phiên duy nhất (sessionId) cho lần đăng nhập này
        String sessionId = UUID.randomUUID().toString();

        // 5. Lưu phiên hoạt động duy nhất này vào Redis (ghi đè mọi phiên cũ của user)
        tokenStorage.saveActiveSession(user.getId(), sessionId);

        // 6. Sinh Access Token mang theo sessionId này và Refresh Token
        String accessToken = tokenProvider.generateAccessToken(user, sessionId);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        // 7. Lưu trữ Refresh Token vào Redis
        tokenStorage.saveRefreshToken(refreshToken, user.getId());

        // 8. Bắn sự kiện người dùng đã đăng nhập ở phiên này
        eventPublisher.publishEvent(new UserLoggedInEvent(
                user.getId(),
                user.getEmail(),
                sessionId,
                Instant.now()
        ));

        // 9. Trả về thông tin đăng nhập thành công
        return AuthResponse.of(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpiration(),
                user
        );
    }
}
