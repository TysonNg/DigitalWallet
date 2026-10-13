package com.tyson.digitalwallet.ddd.infrastructure.persistence.adapter.payment;

import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;
import org.junit.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import static org.junit.Assert.*;

public class StripeWebhookValidationTest {
    private static final String SECRET = "whsec_test_only";
    private final StripePaymentAdapter adapter = new StripePaymentAdapter("", SECRET);
    private String event(String type, String status, String amount, String currency) {
        return "{\"id\":\"evt_test\",\"object\":\"event\",\"api_version\":\"2024-06-20\",\"type\":\"" + type + "\",\"data\":{\"object\":{"
                + "\"id\":\"cs_test\",\"object\":\"checkout.session\",\"payment_status\":" + status
                + ",\"amount_total\":" + amount + ",\"currency\":" + currency + "}}}";
    }
    private Map<String,String> signature(String body) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hash = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
        return Map.of("Stripe-Signature", "t=" + timestamp + ",v1=" + hash);
    }
    @Test public void unpaidCheckoutDoesNotBecomeSuccess() throws Exception {
        String body = event("checkout.session.completed", "\"unpaid\"", "500000", "\"vnd\"");
        assertNotEquals(PaymentOrderStatus.SUCCESS, adapter.verifyWebhook(body, signature(body)).status());
    }
    @Test public void noPaymentRequiredDoesNotFundWallet() throws Exception {
        String body = event("checkout.session.completed", "\"no_payment_required\"", "500000", "\"vnd\"");
        assertNotEquals(PaymentOrderStatus.SUCCESS, adapter.verifyWebhook(body, signature(body)).status());
    }
    @Test public void missingPaymentStatusIsRejected() throws Exception {
        String body = event("checkout.session.completed", "null", "500000", "\"vnd\"");
        assertNotEquals(PaymentOrderStatus.SUCCESS, adapter.verifyWebhook(body, signature(body)).status());
    }
    @Test public void missingCurrencyIsRejected() throws Exception {
        String body = event("checkout.session.completed", "\"paid\"", "500000", "null");
        assertNotEquals(PaymentOrderStatus.SUCCESS, adapter.verifyWebhook(body, signature(body)).status());
    }
    @Test public void missingOrFractionalOrZeroAmountIsRejected() throws Exception {
        for (String amount : new String[]{"null", "500000.5", "0", "-1"}) {
            String body = event("checkout.session.completed", "\"paid\"", amount, "\"vnd\"");
            assertNotEquals(amount, PaymentOrderStatus.SUCCESS, adapter.verifyWebhook(body, signature(body)).status());
        }
    }
    @Test public void paidCheckoutIsAccepted() throws Exception {
        String body = event("checkout.session.completed", "\"paid\"", "500000", "\"vnd\"");
        assertEquals(PaymentOrderStatus.SUCCESS, adapter.verifyWebhook(body, signature(body)).status());
    }
    @Test public void delayedPaymentSuccessIsAccepted() throws Exception {
        String body = event("checkout.session.async_payment_succeeded", "\"paid\"", "500000", "\"vnd\"");
        assertEquals(PaymentOrderStatus.SUCCESS, adapter.verifyWebhook(body, signature(body)).status());
    }
    @Test public void tamperedSignedPayloadIsRejected() throws Exception {
        String body = event("checkout.session.completed", "\"paid\"", "500000", "\"vnd\"");
        assertFalse(adapter.verifyWebhook(body.replace("500000", "999999"), signature(body)).isValid());
    }
}