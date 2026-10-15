package com.tyson.digitalwallet.ddd.application.usecase.auth.impl;

import com.tyson.digitalwallet.ddd.application.usecase.auth.port.PendingRegistrationStore;
import com.tyson.digitalwallet.ddd.application.usecase.auth.port.RegistrationOtpSender;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import java.util.Optional;
import com.tyson.digitalwallet.ddd.application.exception.InvalidOtpException;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.PendingRegistration;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.RegisterCommand;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.VerifyRegisterOtpCommand;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.*;
import com.tyson.digitalwallet.ddd.application.port.security.*;

import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDate;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class RegisterUseCaseImplTest {
    private final UserRepository users = mock(UserRepository.class);
    private final WalletRepository wallets = mock(WalletRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final PasswordHasher passwords = mock(PasswordHasher.class);
    private final TokenStorage tokens = mock(TokenStorage.class);
    private final TokenProvider provider = mock(TokenProvider.class);
    private final OtpStorage otp = mock(OtpStorage.class);
    private final RegistrationOtpSender sender = mock(RegistrationOtpSender.class);
    private final PendingRegistrationStore store = mock(PendingRegistrationStore.class);
    private final WalletService walletService = mock(WalletService.class);
    private final RegisterUseCaseImpl useCase = new RegisterUseCaseImpl(
            users, wallets, events, passwords, tokens, provider, otp, sender, store, walletService);
    private final String email = "user@example.com";

    @Test
    public void registrationStoresHashedPasswordAndOtpBeforeSendingEmail() {
        when(passwords.hash("secret")).thenReturn("hashed");
        when(sender.generateOtp()).thenReturn("123456");
        useCase.register(new RegisterCommand("User", email, "0901234567",
                LocalDate.of(1990, 1, 1), "secret", "secret"));
        var registration = org.mockito.ArgumentCaptor.forClass(PendingRegistration.class);
        verify(store).save(registration.capture(), eq(5L));
        assertEquals("hashed", registration.getValue().hashedPassword());
        assertEquals(email, registration.getValue().email());
        verify(otp).saveOtp(email, "123456", 5L);
        var order = inOrder(store, otp, sender);
        order.verify(store).save(any(), eq(5L));
        order.verify(otp).saveOtp(email, "123456", 5L);
        order.verify(sender).send(email, "User", "123456");
        verifyNoInteractions(wallets, walletService);
    }

    @Test
    public void invalidOtpDoesNotCreateUserOrWallet() {
        assertThrows(InvalidOtpException.class, () -> useCase.verifyOtp(new VerifyRegisterOtpCommand(email, "123456")));
        verifyNoInteractions(users, wallets, walletService, tokens);
    }

    @Test
    public void expiredRegistrationDoesNotCreateUserOrWallet() {
        when(otp.validateOtp(email, "123456")).thenReturn(true);
        assertThrows(InvalidOtpException.class, () -> useCase.verifyOtp(new VerifyRegisterOtpCommand(email, "123456")));
        verifyNoInteractions(users, wallets, walletService, tokens);
    }

    @Test
    public void verifiedRegistrationCreatesWalletAndReturnsAuthentication() {
        when(otp.validateOtp(email, "123456")).thenReturn(true);
        when(store.findByEmail(email)).thenReturn(Optional.of(
                new PendingRegistration("User", email, "0901234567", "1990-01-01", "hashed")));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(provider.generateAccessToken(any(), anyString())).thenReturn("access");
        when(provider.generateRefreshToken(any())).thenReturn("refresh");
        var result = useCase.verifyOtp(new VerifyRegisterOtpCommand(email, "123456"));
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        verify(walletService).createWallet(any(), eq("VND"));
        verify(otp).deleteOtp(email);
        verify(store).delete(email);
    }
}
