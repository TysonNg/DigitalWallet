package com.tyson.digitalwallet.ddd.application.usecase.auth.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.exception.ExistEmailException;
import com.tyson.digitalwallet.ddd.application.exception.InvalidOtpException;
import com.tyson.digitalwallet.ddd.application.usecase.auth.RegisterUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.PendingRegistration;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.RegisterCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.VerifyRegisterOtpCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.response.AuthResponse;
import com.tyson.digitalwallet.ddd.domain.event.UserRegisteredEvent;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.domain.service.security.OtpStorage;
import com.tyson.digitalwallet.ddd.domain.service.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenProvider;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenStorage;
import com.tyson.digitalwallet.ddd.domain.service.sender.MailSender;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

@Service
public class RegisterUseCaseImpl implements RegisterUseCase {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordHasher passwordHasher;
    private final TokenStorage tokenStorage;
    private final TokenProvider tokenProvider;
    private final OtpStorage otpStorage;
    private final MailSender mailSender;
    private final ObjectMapper objectMapper;

    public RegisterUseCaseImpl(
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            PasswordHasher passwordHasher,
            TokenStorage tokenStorage,
            TokenProvider tokenProvider,
            OtpStorage otpStorage,
            MailSender mailSender,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.passwordHasher = passwordHasher;
        this.tokenStorage = tokenStorage;
        this.tokenProvider = tokenProvider;
        this.otpStorage = otpStorage;
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
    }

    @Override
    public void register(RegisterCommand registerCommand) {
        Boolean isExistEmail = userRepository.existByEmail(registerCommand.email());
        if (Boolean.TRUE.equals(isExistEmail)) {
            throw new ExistEmailException();
        }

        String password = registerCommand.password();
        String confirmPassword = registerCommand.confirmPassword();
        if (password == null || !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        if (registerCommand.dob() != null && Period.between(registerCommand.dob(), LocalDate.now()).getYears() < 18) {
            throw new IllegalArgumentException("User must be at least 18 years old to create a wallet");
        }

        // Sinh mã OTP 6 số ngẫu nhiên
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        // Băm mật khẩu trước khi lưu tạm vào Redis
        String hashedPassword = passwordHasher.hash(password);

        PendingRegistration pendingRegistration = new PendingRegistration(
                registerCommand.fullName(),
                registerCommand.email(),
                registerCommand.phoneNumber(),
                registerCommand.dob() != null ? registerCommand.dob().toString() : null,
                hashedPassword
        );

        try {
            String pendingJson = objectMapper.writeValueAsString(pendingRegistration);
            // Lưu OTP và dữ liệu đăng ký tạm vào Redis (hết hạn trong 5 phút)
            otpStorage.savePendingRegistration(registerCommand.email(), pendingJson, 5);
            otpStorage.saveOtp(registerCommand.email(), otp, 5);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to store pending registration data", e);
        }

        // Gửi email chứa mã OTP
        mailSender.sendTemplate(
                registerCommand.email(),
                "Account Registration OTP - Digital Wallet",
                "mail/otp-email",
                Map.of(
                        "fullName", registerCommand.fullName() != null ? registerCommand.fullName() : "Valued Customer",
                        "otpCode", otp
                )
        );
    }

    @Override
    public AuthResponse verifyOtp(VerifyRegisterOtpCommand verifyCommand) {
        String email = verifyCommand.email();
        String inputOtp = verifyCommand.otp();

        boolean isValid = otpStorage.validateOtp(email, inputOtp);
        if (!isValid) {
            throw new InvalidOtpException("Invalid or expired OTP!");
        }

        String pendingJson = otpStorage.getPendingRegistration(email);
        if (pendingJson == null) {
            throw new InvalidOtpException("Registration session has expired. Please register again!");
        }

        PendingRegistration pending;
        try {
            pending = objectMapper.readValue(pendingJson, PendingRegistration.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process registration data", e);
        }

        User user = User.create(
                pending.fullName(),
                pending.email(),
                pending.hashedPassword(),
                pending.phoneNumber(),
                pending.getLocalDateDob()
        );

        if (!user.canCreateWallet()) {
            throw new IllegalArgumentException("User must be at least 18 years old to create a wallet");
        }

        User savedUser = userRepository.save(user);

        // Xóa OTP và dữ liệu tạm khỏi Redis sau khi đã dùng xong
        otpStorage.deleteOtp(email);
        otpStorage.deletePendingRegistration(email);

        // Tạo mã phiên duy nhất (sessionId)
        String sessionId = java.util.UUID.randomUUID().toString();
        tokenStorage.saveActiveSession(savedUser.getId(), sessionId);

        // Tạo Access Token và Refresh Token
        String accessToken = tokenProvider.generateAccessToken(savedUser, sessionId);
        String refreshToken = tokenProvider.generateRefreshToken(savedUser);

        tokenStorage.saveRefreshToken(refreshToken, savedUser.getId());

        // Bắn sự kiện người dùng đã đăng ký thành công -> UserRegisteredEventListener sẽ gửi welcome-email
        eventPublisher.publishEvent(new UserRegisteredEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                Instant.now()
        ));

        return AuthResponse.of(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpiration(),
                savedUser
        );
    }
}
