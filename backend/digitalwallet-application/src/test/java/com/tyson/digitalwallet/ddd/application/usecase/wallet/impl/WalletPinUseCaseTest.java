package com.tyson.digitalwallet.ddd.application.usecase.wallet.impl;

import com.tyson.digitalwallet.ddd.application.exception.AppException;
import com.tyson.digitalwallet.ddd.application.exception.ErrorCode;
import com.tyson.digitalwallet.ddd.application.port.security.PasswordHasher;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.ChangeWalletPinCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.command.SetupWalletPinCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.pin.WalletPinAttemptService;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WalletPinUseCaseTest {

    private final WalletRepository walletRepository = mock(WalletRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final WalletPinAttemptService pinAttemptService = mock(WalletPinAttemptService.class);

    private final SetupWalletPinUseCaseImpl setupUseCase = new SetupWalletPinUseCaseImpl(
            walletRepository, passwordHasher, pinAttemptService
    );

    private final ChangeWalletPinUseCaseImpl changeUseCase = new ChangeWalletPinUseCaseImpl(
            walletRepository, passwordHasher, pinAttemptService
    );

    @Test
    public void setupPinSuccessfullyHashesAndSavesPin() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, userId, BigDecimal.valueOf(1000000), "VND", WalletStatus.ACTIVE, null, LocalDateTime.now(), LocalDateTime.now());

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(passwordHasher.hash("159357")).thenReturn("hashed_pin_159357");

        setupUseCase.setupPin(new SetupWalletPinCommand(userId, walletId, "159357"));

        assertTrue(wallet.hasPin());
        assertEquals("hashed_pin_159357", wallet.getPinHash());
        verify(walletRepository).saveWallet(wallet);
        verify(pinAttemptService).resetAttempts(walletId);
    }

    @Test
    public void setupPinAllowsCustomAndRepeatingDigits() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, userId, BigDecimal.valueOf(1000000), "VND", WalletStatus.ACTIVE, null, LocalDateTime.now(), LocalDateTime.now());

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(passwordHasher.hash("111111")).thenReturn("hashed_pin_111111");

        setupUseCase.setupPin(new SetupWalletPinCommand(userId, walletId, "111111"));

        assertTrue(wallet.hasPin());
        assertEquals("hashed_pin_111111", wallet.getPinHash());
        verify(walletRepository).saveWallet(wallet);
    }

    @Test
    public void setupPinFailsIfInvalidFormat() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, userId, BigDecimal.valueOf(1000000), "VND", WalletStatus.ACTIVE, null, LocalDateTime.now(), LocalDateTime.now());

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        AppException ex = assertThrows(AppException.class, () ->
                setupUseCase.setupPin(new SetupWalletPinCommand(userId, walletId, "12345"))
        );

        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        verify(walletRepository, never()).saveWallet(any());
    }

    @Test
    public void setupPinFailsIfWalletAlreadyHasPin() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, userId, BigDecimal.valueOf(1000000), "VND", WalletStatus.ACTIVE, "existing_pin", LocalDateTime.now(), LocalDateTime.now());

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        AppException ex = assertThrows(AppException.class, () ->
                setupUseCase.setupPin(new SetupWalletPinCommand(userId, walletId, "159357"))
        );

        assertEquals(ErrorCode.WALLET_ALREADY_HAS_PIN, ex.getErrorCode());
        verify(walletRepository, never()).saveWallet(any());
    }

    @Test
    public void changePinSuccessfullyUpdatesPinWhenOldPinMatches() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, userId, BigDecimal.valueOf(1000000), "VND", WalletStatus.ACTIVE, "old_hashed_pin", LocalDateTime.now(), LocalDateTime.now());

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(passwordHasher.matches("159357", "old_hashed_pin")).thenReturn(true);
        when(passwordHasher.matches("852456", "old_hashed_pin")).thenReturn(false);
        when(passwordHasher.hash("852456")).thenReturn("new_hashed_pin");

        changeUseCase.changePin(new ChangeWalletPinCommand(userId, walletId, "159357", "852456"));

        assertEquals("new_hashed_pin", wallet.getPinHash());
        verify(walletRepository).saveWallet(wallet);
        verify(pinAttemptService).resetAttempts(walletId);
    }

    @Test
    public void changePinRecordsFailedAttemptWhenOldPinIsWrong() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, userId, BigDecimal.valueOf(1000000), "VND", WalletStatus.ACTIVE, "old_hashed_pin", LocalDateTime.now(), LocalDateTime.now());

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(passwordHasher.matches("wrong_pin", "old_hashed_pin")).thenReturn(false);
        when(pinAttemptService.recordFailedAttempt(walletId)).thenReturn(4);

        AppException ex = assertThrows(AppException.class, () ->
                changeUseCase.changePin(new ChangeWalletPinCommand(userId, walletId, "wrong_pin", "852456"))
        );

        assertEquals(ErrorCode.INVALID_WALLET_PIN, ex.getErrorCode());
        verify(pinAttemptService).recordFailedAttempt(walletId);
        verify(walletRepository, never()).saveWallet(any());
    }
}
