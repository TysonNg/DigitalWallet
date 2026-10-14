package com.tyson.digitalwallet.ddd.infrastructure.persistence.adapter.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;
import com.tyson.digitalwallet.ddd.infrastructure.adapter.payment.PayOsPaymentAdapter;
import vn.payos.crypto.CryptoProviderImpl;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.Assert.*;

public class PayOsWebhookValidationTest {
    @Test public void verifiedPayOsAmountAndCurrencyReachValidation() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String checksum = "test-only-checksum";
        var adapter = new PayOsPaymentAdapter("test-client", "test-api-key", checksum, mapper);
        var data = WebhookData.builder().orderCode(12345L).amount(500000L).currency("VND")
                .description("Deposit test").accountNumber("123456").reference("test-reference").transactionDateTime("2026-10-12 23:00:00").paymentLinkId("test-link").code("00").desc("success").build();
        String signature = new CryptoProviderImpl().createSignatureFromObj(data, checksum);
        var webhook = new Webhook("00", "success", true, data, signature);
        var result = adapter.verifyWebhook(mapper.writeValueAsString(webhook), Map.of());
        assertTrue(result.message(), result.isValid());
        assertEquals(PaymentOrderStatus.SUCCESS, result.status());
        assertEquals(new BigDecimal("500000"), result.amount());
        assertEquals("VND", result.currency());
    }
    @Test public void tamperedPayOsDataCannotReachCreditValidation() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String checksum = "test-only-checksum";
        var adapter = new PayOsPaymentAdapter("test-client", "test-api-key", checksum, mapper);
        var data = WebhookData.builder().orderCode(12345L).amount(500000L).currency("VND")
                .description("Deposit test").accountNumber("123456").reference("test-reference").transactionDateTime("2026-10-12 23:00:00").paymentLinkId("test-link").code("00").desc("success").build();
        String signature = new CryptoProviderImpl().createSignatureFromObj(data, checksum);
        data.setAmount(999999L);
        var result = adapter.verifyWebhook(mapper.writeValueAsString(new Webhook("00", "success", true, data, signature)), Map.of());
        assertFalse(result.isValid());
    }
}