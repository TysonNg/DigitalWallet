package com.tyson.digitalwallet.ddd.infrastructure.persistence.adapter.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.domain.exception.PaymentGatewayUnavailableException;
import com.tyson.digitalwallet.ddd.domain.exception.ServiceUnavailableException;
import com.tyson.digitalwallet.ddd.application.port.payment.command.CreatePaymentLinkCommand;
import com.tyson.digitalwallet.ddd.infrastructure.adapter.MailSenderImpl;
import com.tyson.digitalwallet.ddd.infrastructure.adapter.payment.PayOsPaymentAdapter;
import com.tyson.digitalwallet.ddd.infrastructure.adapter.payment.StripePaymentAdapter;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CircuitBreakerFallbackTest {

    @Test
    public void testPayOsFallback_ThrowsPaymentGatewayUnavailableException() {
        PayOsPaymentAdapter adapter = new PayOsPaymentAdapter("fake-client", "fake-api", "fake-checksum", new ObjectMapper());
        CreatePaymentLinkCommand cmd = new CreatePaymentLinkCommand(
                UUID.randomUUID(), new BigDecimal("100000"), "Test PayOS", "http://return", "http://cancel");

        try {
            adapter.createPaymentLinkFallback(cmd, new RuntimeException("PayOS network connection timeout"));
            fail("Expected PaymentGatewayUnavailableException");
        } catch (PaymentGatewayUnavailableException ex) {
            assertTrue(ex.getMessage().contains("PayOS"));
            assertTrue(ex.getMessage().contains("tạm thời gián đoạn"));
        }
    }

    @Test
    public void testStripeFallback_ThrowsPaymentGatewayUnavailableException() {
        StripePaymentAdapter adapter = new StripePaymentAdapter("sk_test_fake", "whsec_fake");
        CreatePaymentLinkCommand cmd = new CreatePaymentLinkCommand(
                UUID.randomUUID(), new BigDecimal("500000"), "Test Stripe", "http://return", "http://cancel");

        try {
            adapter.createPaymentLinkFallback(cmd, new RuntimeException("Stripe API gateway down"));
            fail("Expected PaymentGatewayUnavailableException");
        } catch (PaymentGatewayUnavailableException ex) {
            assertTrue(ex.getMessage().contains("Stripe"));
            assertTrue(ex.getMessage().contains("tạm thời gián đoạn"));
        }
    }

    @Test
    public void testMailSenderFallback_ThrowsServiceUnavailableException() {
        MailSenderImpl mailSenderImpl = new MailSenderImpl(null, null);

        try {
            mailSenderImpl.sendFallback("user@example.com", "Test Subject", "Test Content", new RuntimeException("SMTP connect timeout"));
            fail("Expected ServiceUnavailableException");
        } catch (ServiceUnavailableException ex) {
            assertTrue(ex.getMessage().contains("email"));
            assertTrue(ex.getMessage().contains("tạm thời gián đoạn"));
        }

        try {
            mailSenderImpl.sendTemplateFallback("user@example.com", "OTP Code", "otp-template", Map.of(), new RuntimeException("Thymeleaf/SMTP error"));
            fail("Expected ServiceUnavailableException");
        } catch (ServiceUnavailableException ex) {
            assertTrue(ex.getMessage().contains("email"));
            assertTrue(ex.getMessage().contains("tạm thời gián đoạn"));
        }
    }
}
