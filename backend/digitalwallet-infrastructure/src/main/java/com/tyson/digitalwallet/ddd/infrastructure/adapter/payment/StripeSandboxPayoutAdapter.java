package com.tyson.digitalwallet.ddd.infrastructure.adapter.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.Payout;
import com.stripe.model.Transfer;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.tyson.digitalwallet.ddd.application.port.payment.StripePayoutPort;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/** Sandbox-only: published test bank numbers and explicit test credentials. */
@Component
public class StripeSandboxPayoutAdapter implements StripePayoutPort {
    private final String secretKey;
    private final String webhookSecret;
    private final ObjectMapper json = new ObjectMapper();

    public StripeSandboxPayoutAdapter(
            @Value("${payment.stripe.secret-key:}") String secretKey,
            @Value("${payment.stripe.connect-webhook-secret:}") String webhookSecret) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        if (secretKey != null && !secretKey.isBlank() && !isTestKey(secretKey)) {
            throw new IllegalArgumentException(
                    "Only Stripe sk_test_/rk_test_ credentials are allowed for demo payouts");
        }
    }

    private static boolean isTestKey(String key) {
        return key.startsWith("sk_test_") || key.startsWith("rk_test_");
    }

    @Override
    public void assertTestConfiguration() {
        if (secretKey == null || !isTestKey(secretKey))
            throw new IllegalStateException("Configure STRIPE_SECRET_KEY with a sandbox test key");
        if (webhookSecret == null || !webhookSecret.startsWith("whsec_")) {
            throw new IllegalStateException(
                    "Configure STRIPE_CONNECT_WEBHOOK_SECRET before reserving demo funds");
        }
    }

    private RequestOptions options(String account, String key) {
        assertTestConfiguration();
        var options =
                RequestOptions.builder()
                        .setApiKey(secretKey)
                        .setConnectTimeout(5000)
                        .setReadTimeout(10000)
                        .setMaxNetworkRetries(2);
        if (account != null) options.setStripeAccount(account);
        if (key != null) options.setIdempotencyKey(key);
        return options.build();
    }

    private ProviderException failure(StripeException ex) {
        Integer code = ex.getStatusCode();
        boolean retry = code == null || code >= 500 || code == 409 || code == 429;
        return new ProviderException(
                "Stripe sandbox request "
                        + (retry ? "temporarily unavailable" : "rejected")
                        + (ex.getCode() == null ? "" : " (" + ex.getCode() + ")"),
                retry,
                ex);
    }

    private void requireTest(Boolean live) {
        if (!Boolean.FALSE.equals(live))
            throw new ProviderException("Stripe returned a non-test object", true, null);
    }

    @Override
    public String createTestAccount(UUID id, String bank) {
        if (!Set.of("SUCCESS", "FAILURE").contains(bank))
            throw new IllegalArgumentException("Unknown Stripe test bank");
        Map<String, Object> individual = new HashMap<>();
        individual.put("first_name", "Jenny");
        individual.put("last_name", "Rosen");
        individual.put("email", "stripe-demo@example.com");
        individual.put("phone", "0000000000");
        individual.put("dob", Map.of("day", 1, "month", 1, "year", 1901));
        individual.put("ssn_last_4", "0000");
        individual.put(
                "address",
                Map.of(
                        "line1",
                        "address_full_match",
                        "city",
                        "New York",
                        "state",
                        "NY",
                        "postal_code",
                        "10001",
                        "country",
                        "US"));
        Map<String, Object> params = new HashMap<>();
        params.put("type", "custom");
        params.put("country", "US");
        params.put("default_currency", "usd");
        params.put("business_type", "individual");
        params.put("individual", individual);
        params.put("capabilities", Map.of("transfers", Map.of("requested", true)));
        params.put(
                "business_profile",
                Map.of(
                        "mcc",
                        "5734",
                        "url",
                        "https://example.com",
                        "product_description",
                        "Digital Wallet sandbox demonstration"));
        // Stable parameters are required for replaying the same Stripe idempotency key.
        params.put("tos_acceptance", Map.of("date", 1609459200L, "ip", "127.0.0.1"));
        params.put("settings", Map.of("payouts", Map.of("schedule", Map.of("interval", "manual"))));
        params.put(
                "external_account",
                Map.of(
                        "object",
                        "bank_account",
                        "country",
                        "US",
                        "currency",
                        "usd",
                        "routing_number",
                        "110000000",
                        "account_number",
                        "SUCCESS".equals(bank) ? "000123456789" : "000111111116",
                        "account_holder_name",
                        "Jenny Rosen",
                        "account_holder_type",
                        "individual"));
        params.put("metadata", Map.of("withdrawal_id", id.toString(), "mode", "STRIPE_TEST"));
        try {
            return Account.create(params, options(null, "dw-account-" + id)).getId();
        } catch (StripeException ex) {
            throw failure(ex);
        }
    }

    @Override
    public String transfer(UUID id, String accountId, long cents) {
        try {
            var transfer =
                    Transfer.create(
                            Map.of(
                                    "amount",
                                    cents,
                                    "currency",
                                    "usd",
                                    "destination",
                                    accountId,
                                    "metadata",
                                    Map.of("withdrawal_id", id.toString())),
                            options(null, "dw-transfer-" + id));
            requireTest(transfer.getLivemode());
            return transfer.getId();
        } catch (StripeException ex) {
            throw failure(ex);
        }
    }

    @Override
    public String payout(UUID id, String accountId, long cents) {
        try {
            var payout =
                    Payout.create(
                            Map.of(
                                    "amount",
                                    cents,
                                    "currency",
                                    "usd",
                                    "method",
                                    "standard",
                                    "metadata",
                                    Map.of("withdrawal_id", id.toString())),
                            options(accountId, "dw-payout-" + id));
            requireTest(payout.getLivemode());
            return payout.getId();
        } catch (StripeException ex) {
            if ("balance_insufficient".equals(ex.getCode()))
                throw new ProviderException(
                        "Waiting for Stripe test balance availability", true, ex);
            throw failure(ex);
        }
    }

    @Override
    public PayoutState retrieve(String accountId, String payoutId) {
        try {
            var payout = Payout.retrieve(payoutId, options(accountId, null));
            requireTest(payout.getLivemode());
            return new PayoutState(
                    payout.getId(),
                    accountId,
                    payout.getAmount(),
                    payout.getCurrency(),
                    payout.getStatus());
        } catch (StripeException ex) {
            throw failure(ex);
        }
    }

    @Override
    public void reverseTransfer(UUID id, String transferId) {
        try {
            var transfer = Transfer.retrieve(transferId, options(null, null));
            requireTest(transfer.getLivemode());
            // Recover even after Stripe has expired the reversal idempotency key.
            if (Boolean.TRUE.equals(transfer.getReversed())
                    || (transfer.getAmount() != null
                            && transfer.getAmount().equals(transfer.getAmountReversed()))) return;
            transfer.getReversals().create(Map.of(), options(null, "dw-reversal-" + id));
        } catch (StripeException ex) {
            throw failure(ex);
        }
    }

    @Override
    public WebhookNotice verifyWebhook(String body, String signature) {
        assertTestConfiguration();
        if (signature == null || signature.isBlank())
            throw new IllegalArgumentException("Missing Stripe-Signature");
        try {
            var event = Webhook.constructEvent(body, signature, webhookSecret);
            var root = json.readTree(body);
            if (!root.path("livemode").isBoolean() || root.path("livemode").booleanValue()) {
                throw new IllegalArgumentException("Live Stripe events are forbidden");
            }
            if (!Set.of(
                            "payout.created",
                            "payout.updated",
                            "payout.paid",
                            "payout.failed",
                            "payout.canceled")
                    .contains(event.getType())) return null;
            var payout = root.path("data").path("object");
            if (!payout.path("livemode").isBoolean()
                    || payout.path("livemode").booleanValue()
                    || !"payout".equals(payout.path("object").asText()))
                throw new IllegalArgumentException("Expected a test payout object");
            String request = payout.path("metadata").path("withdrawal_id").asText();
            if (request.isBlank()) return null;
            String account = root.path("account").asText();
            String payoutId = payout.path("id").asText();
            var amount = payout.path("amount");
            if (!account.startsWith("acct_")
                    || !payoutId.startsWith("po_")
                    || !amount.isIntegralNumber()
                    || !amount.canConvertToLong()
                    || amount.longValue() <= 0
                    || !"usd".equals(payout.path("currency").asText())) {
                throw new IllegalArgumentException("Invalid Stripe Connect payout payload");
            }
            return new WebhookNotice(
                    UUID.fromString(request),
                    account,
                    payoutId,
                    amount.longValue(),
                    "usd",
                    event.getId());
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Invalid Stripe Connect webhook signature or payload", ex);
        }
    }
}
