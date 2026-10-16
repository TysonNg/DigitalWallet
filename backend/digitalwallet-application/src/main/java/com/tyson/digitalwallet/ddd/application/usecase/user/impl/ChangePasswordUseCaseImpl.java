package com.tyson.digitalwallet.ddd.application.usecase.user.impl;

import com.tyson.digitalwallet.ddd.application.exception.AppException;
import com.tyson.digitalwallet.ddd.application.exception.ErrorCode;
import com.tyson.digitalwallet.ddd.application.port.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.application.port.security.TokenStorage;
import com.tyson.digitalwallet.ddd.application.usecase.user.ChangePasswordUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.user.command.ChangePasswordCommand;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordUseCaseImpl implements ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenStorage tokenStorage;

    public ChangePasswordUseCaseImpl(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenStorage tokenStorage
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenStorage = tokenStorage;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        if (command == null || command.userId() == null) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS, "User ID is required");
        }

        if (command.currentPassword() == null || command.currentPassword().isBlank()) {
            throw new AppException(ErrorCode.INVALID_OLD_PASSWORD, "Current password cannot be blank");
        }

        if (command.newPassword() == null || command.newPassword().length() < 6) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "New password must have at least 6 characters");
        }

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found with id: " + command.userId()));

        if (!user.isActive()) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE, "User account is not active");
        }

        if (!passwordHasher.matches(command.currentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_OLD_PASSWORD, "Current password is not correct");
        }

        if (passwordHasher.matches(command.newPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD, "New password cannot be the same as the old password");
        }

        String hashedNewPassword = passwordHasher.hash(command.newPassword());
        user.changePassword(hashedNewPassword);
        userRepository.save(user);

        // Revoke existing refresh token to force re-authentication or invalidate old sessions
        tokenStorage.deleteRefreshToken(command.userId());
    }
}
