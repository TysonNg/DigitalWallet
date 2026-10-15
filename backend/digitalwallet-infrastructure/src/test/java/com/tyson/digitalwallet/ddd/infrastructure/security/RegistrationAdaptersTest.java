package com.tyson.digitalwallet.ddd.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.usecase.auth.command.PendingRegistration;
import com.tyson.digitalwallet.ddd.infrastructure.cached.RedisInfrasService;
import java.util.concurrent.TimeUnit;
import com.tyson.digitalwallet.ddd.application.port.sender.MailSender;
import com.tyson.digitalwallet.ddd.infrastructure.adapter.EmailRegistrationOtpSender;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RegistrationAdaptersTest {
    private final RedisInfrasService storage = mock(RedisInfrasService.class);
    private final JsonPendingRegistrationStore store = new JsonPendingRegistrationStore(storage, new ObjectMapper());
    private final String email = "user@example.com";

    @Test
    public void pendingRegistrationRoundTripsUsingExistingJsonFormat() throws Exception {
        AtomicReference<String> persistedJson = new AtomicReference<>();
        doAnswer(invocation -> {
            persistedJson.set(invocation.getArgument(1));
            return null;
        }).when(storage).set(eq("pending:register:" + email), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        when(storage.get("pending:register:" + email)).thenAnswer(invocation -> persistedJson.get());
        var registration = new PendingRegistration("User", email, "0901234567", "1990-01-01", "hashed");
        store.save(registration, 5);
        assertEquals(registration, store.findByEmail(email).orElseThrow());
        var json = new ObjectMapper().readTree(persistedJson.get());
        assertEquals("hashed", json.get("hashedPassword").asText());
        assertFalse(json.has("localDateDob"));
    }

    @Test
    public void missingRegistrationReturnsEmpty() {
        assertTrue(store.findByEmail(email).isEmpty());
    }

    @Test
    public void malformedRegistrationPreservesCause() {
        when(storage.get("pending:register:" + email)).thenReturn("invalid-json");
        var failure = assertThrows(IllegalStateException.class, () -> store.findByEmail(email));
        assertNotNull(failure.getCause());
    }

    @Test
    public void deletesPendingRegistration() {
        store.delete(email);
        verify(storage).delete("pending:register:" + email);
    }

    @Test
    public void generatesSixAsciiDigitsRegardlessOfLocale() {
        var sender = new EmailRegistrationOtpSender(mock(MailSender.class));
        for (int i = 0; i < 100; i++) {
            assertTrue(sender.generateOtp().matches("[0-9]{6}"));
        }
    }

    @Test
    public void sendsExistingTemplateWithFallbackName() {
        MailSender mail = mock(MailSender.class);
        var sender = new EmailRegistrationOtpSender(mail);
        sender.send(email, null, "000123");
        verify(mail).sendTemplate(email, "Account Registration OTP - Digital Wallet", "mail/otp-email",
                Map.of("fullName", "Valued Customer", "otpCode", "000123"));
    }
}