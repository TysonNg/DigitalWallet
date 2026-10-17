package com.tyson.digitalwallet.ddd.application.usecase.auth.impl;

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
import com.tyson.digitalwallet.ddd.application.port.security.OtpStorage;
import com.tyson.digitalwallet.ddd.application.port.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.application.port.security.TokenProvider;
import com.tyson.digitalwallet.ddd.application.port.security.TokenStorage;
import com.tyson.digitalwallet.ddd.application.usecase.auth.port.PendingRegistrationStore;
import com.tyson.digitalwallet.ddd.application.usecase.auth.port.RegistrationOtpSender;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Service
public class RegisterUseCaseImpl implements RegisterUseCase {

    private static final long OTP_EXPIRATION_MINUTES = 5;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordHasher passwordHasher;
    private final TokenStorage tokenStorage;
    private final TokenProvider tokenProvider;
    private final OtpStorage otpStorage;
    private final RegistrationOtpSender otpSender;
    private final PendingRegistrationStore pendingRegistrations;
    private final WalletService walletService;

    public RegisterUseCaseImpl(
            UserRepository userRepository,
            WalletRepository walletRepository,
            ApplicationEventPublisher eventPublisher,
            PasswordHasher passwordHasher,
            TokenStorage tokenStorage,
            TokenProvider tokenProvider,
            OtpStorage otpStorage,
            RegistrationOtpSender otpSender,
            PendingRegistrationStore pendingRegistrations,
            WalletService walletService
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
        this.passwordHasher = passwordHasher;
        this.tokenStorage = tokenStorage;
        this.tokenProvider = tokenProvider;
        this.otpStorage = otpStorage;
        this.otpSender = otpSender;
        this.pendingRegistrations = pendingRegistrations;
        this.walletService = walletService;
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
        String otp = otpSender.generateOtp();

        // Băm mật khẩu trước khi lưu tạm vào Redis
        String hashedPassword = passwordHasher.hash(password);

        PendingRegistration pendingRegistration = new PendingRegistration(
                registerCommand.fullName(),
                registerCommand.email(),
                registerCommand.phoneNumber(),
                registerCommand.dob() != null ? registerCommand.dob().toString() : null,
                hashedPassword
        );

        pendingRegistrations.save(pendingRegistration, OTP_EXPIRATION_MINUTES);
        otpStorage.saveOtp(registerCommand.email(), otp, OTP_EXPIRATION_MINUTES);
        otpSender.send(registerCommand.email(), registerCommand.fullName(), otp);
    }

    @Override
    public AuthResponse verifyOtp(VerifyRegisterOtpCommand verifyCommand) {
        String email = verifyCommand.email();
        String inputOtp = verifyCommand.otp();

        boolean isValid = otpStorage.validateOtp(email, inputOtp);
        if (!isValid) {
            throw new InvalidOtpException("Invalid or expired OTP!");
        }

        PendingRegistration pending = pendingRegistrations.findByEmail(email)
                .orElseThrow(() -> new InvalidOtpException(
                        "Registration session has expired. Please register again!"));

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

        // Tự động khởi tạo Ví VND cho người dùng mới
        if (!walletRepository.existsByUserId(savedUser.getId())) {
            walletService.createWallet(savedUser.getId(), "VND");
        }

        // Xóa OTP và dữ liệu tạm khỏi Redis sau khi đã dùng xong
        otpStorage.deleteOtp(email);
        pendingRegistrations.delete(email);

        // Tạo mã phiên duy nhất (sessionId)
        String sessionId = UUID.randomUUID().toString();
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
