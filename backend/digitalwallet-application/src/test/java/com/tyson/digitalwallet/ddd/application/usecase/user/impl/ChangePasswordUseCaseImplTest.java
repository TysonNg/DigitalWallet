package com.tyson.digitalwallet.ddd.application.usecase.user.impl;

import com.tyson.digitalwallet.ddd.application.exception.AppException;
import com.tyson.digitalwallet.ddd.application.exception.ErrorCode;
import com.tyson.digitalwallet.ddd.application.port.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.application.port.security.TokenStorage;
import com.tyson.digitalwallet.ddd.application.usecase.user.command.ChangePasswordCommand;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ChangePasswordUseCaseImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final TokenStorage tokenStorage = mock(TokenStorage.class);

    private final ChangePasswordUseCaseImpl useCase = new ChangePasswordUseCaseImpl(
            userRepository,
            passwordHasher,
            tokenStorage
    );

    @Test
    public void changePasswordSuccessfullyUpdatesPasswordAndRevokesRefreshToken() {
        UUID userId = UUID.randomUUID();
        User user = User.create("Tyson Ng", "tyson@example.com", "old_hashed_password", "0901234567", LocalDate.of(1995, 5, 20));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("current_pwd_123", "old_hashed_password")).thenReturn(true);
        when(passwordHasher.matches("new_pwd_456", "old_hashed_password")).thenReturn(false);
        when(passwordHasher.hash("new_pwd_456")).thenReturn("new_hashed_password");

        useCase.changePassword(new ChangePasswordCommand(userId, "current_pwd_123", "new_pwd_456"));

        assertEquals("new_hashed_password", user.getPassword());
        verify(userRepository).save(user);
        verify(tokenStorage).deleteRefreshToken(userId);
    }

    @Test
    public void changePasswordFailsWhenCurrentPasswordIsIncorrect() {
        UUID userId = UUID.randomUUID();
        User user = User.create("Tyson Ng", "tyson@example.com", "old_hashed_password", "0901234567", LocalDate.of(1995, 5, 20));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong_pwd", "old_hashed_password")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () ->
                useCase.changePassword(new ChangePasswordCommand(userId, "wrong_pwd", "new_pwd_456"))
        );

        assertEquals(ErrorCode.INVALID_OLD_PASSWORD, ex.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(tokenStorage, never()).deleteRefreshToken(any());
    }

    @Test
    public void changePasswordFailsWhenNewPasswordIsSameAsOldPassword() {
        UUID userId = UUID.randomUUID();
        User user = User.create("Tyson Ng", "tyson@example.com", "old_hashed_password", "0901234567", LocalDate.of(1995, 5, 20));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("same_pwd_123", "old_hashed_password")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () ->
                useCase.changePassword(new ChangePasswordCommand(userId, "same_pwd_123", "same_pwd_123"))
        );

        assertEquals(ErrorCode.NEW_PASSWORD_SAME_AS_OLD, ex.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(tokenStorage, never()).deleteRefreshToken(any());
    }

    @Test
    public void changePasswordFailsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                useCase.changePassword(new ChangePasswordCommand(userId, "current_pwd_123", "new_pwd_456"))
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }
}
