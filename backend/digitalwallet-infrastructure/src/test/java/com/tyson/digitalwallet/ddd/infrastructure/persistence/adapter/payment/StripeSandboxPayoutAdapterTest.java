package com.tyson.digitalwallet.ddd.infrastructure.persistence.adapter.payment;

import static org.junit.Assert.*;

import com.stripe.Stripe;
import com.sun.net.httpserver.HttpServer;
import com.tyson.digitalwallet.ddd.infrastructure.adapter.payment.StripeSandboxPayoutAdapter;

import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class StripeSandboxPayoutAdapterTest {
    private static final String SECRET = "whsec_connect_test_only";
    private final StripeSandboxPayoutAdapter adapter =
            new StripeSandboxPayoutAdapter("sk_test_local_fixture", SECRET);
    private final UUID id = UUID.randomUUID();

    private String event(boolean live) {
        return """
        {"id":"evt_fixture","object":"event","api_version":"2024-12-18.acacia",
        "type":"payout.paid","livemode":%s,"account":"acct_fixture",
        "data":{"object":{"id":"po_fixture","object":"payout","livemode":%s,
        "amount":800,"currency":"usd","status":"paid","metadata":{"withdrawal_id":"%s"}}}}
        """
                .formatted(live, live, id);
    }

    private String sign(String body) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "t="
                + timestamp
                + ",v1="
                + HexFormat.of()
                        .formatHex(
                                mac.doFinal(
                                        (timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void liveCredentialsAreRejectedBeforeAnyRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StripeSandboxPayoutAdapter("sk_live_forbidden", SECRET));
        assertThrows(
                IllegalArgumentException.class,
                () -> new StripeSandboxPayoutAdapter("rk_live_forbidden", SECRET));
        assertThrows(
                IllegalStateException.class,
                () -> new StripeSandboxPayoutAdapter("", SECRET).assertTestConfiguration());
        assertThrows(
                IllegalStateException.class,
                () ->
                        new StripeSandboxPayoutAdapter("sk_test_fixture", "")
                                .assertTestConfiguration());
    }

    @Test
    public void verifiesSignedConnectEventAndRejectsTamperingAndLiveMode() throws Exception {
        String body = event(false);
        var notice = adapter.verifyWebhook(body, sign(body));
        assertEquals(id, notice.withdrawalId());
        assertEquals("acct_fixture", notice.accountId());
        assertEquals(800L, notice.amount());
        String signature = sign(body);
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.verifyWebhook(body.replace("800", "801"), signature));
        String live = event(true), liveSignature = sign(live);
        assertThrows(
                IllegalArgumentException.class, () -> adapter.verifyWebhook(live, liveSignature));
        assertThrows(IllegalArgumentException.class, () -> adapter.verifyWebhook(body, null));
    }

    @Test
    public void apiRequestsUseTestBankScopedAccountAndStableIdempotencyKeys() throws Exception {
        List<String> seen = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/",
                exchange -> {
                    String path = exchange.getRequestURI().getPath();
                    String body =
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8);
                    seen.add(
                            path
                                    + "|"
                                    + exchange.getRequestHeaders().getFirst("Idempotency-Key")
                                    + "|"
                                    + exchange.getRequestHeaders().getFirst("Stripe-Account")
                                    + "|"
                                    + java.net.URLDecoder.decode(body, StandardCharsets.UTF_8));
                    String response;
                    if (path.equals("/v1/accounts"))
                        response =
                                "{\"id\":\"acct_fixture\",\"object\":\"account\",\"country\":\"US\"}";
                    else if (path.equals("/v1/transfers"))
                        response =
                                "{\"id\":\"tr_fixture\",\"object\":\"transfer\",\"amount\":800,\"currency\":\"usd\",\"livemode\":false}";
                    else
                        response =
                                "{\"id\":\"po_fixture\",\"object\":\"payout\",\"amount\":800,\"currency\":\"usd\",\"status\":\"paid\",\"livemode\":false}";
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
        String original = Stripe.getApiBase();
        server.start();
        Stripe.overrideApiBase("http://127.0.0.1:" + server.getAddress().getPort());
        try {
            assertEquals("acct_fixture", adapter.createTestAccount(id, "FAILURE"));
            assertEquals("tr_fixture", adapter.transfer(id, "acct_fixture", 800));
            assertEquals("po_fixture", adapter.payout(id, "acct_fixture", 800));
            assertEquals("paid", adapter.retrieve("acct_fixture", "po_fixture").status());
            assertTrue(seen.get(0).contains("external_account[account_number]=000111111116"));
            assertTrue(seen.get(0).contains("external_account[routing_number]=110000000"));
            assertTrue(seen.get(1).contains("dw-transfer-" + id));
            assertTrue(seen.get(2).contains("dw-payout-" + id + "|acct_fixture|"));
            assertTrue(seen.get(2).contains("amount=800"));
            assertTrue(seen.get(3).contains("acct_fixture"));
        } finally {
            Stripe.overrideApiBase(original);
            server.stop(0);
        }
    }

    @Test
    public void alreadyReversedTransferAllowsRefundRecoveryWithoutAnotherReversal()
            throws Exception {
        List<String> calls = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/",
                exchange -> {
                    calls.add(
                            exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
                    byte[] response =
                            "{\"id\":\"tr_fixture\",\"object\":\"transfer\",\"amount\":800,\"amount_reversed\":800,\"reversed\":true,\"currency\":\"usd\",\"livemode\":false}"
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });
        String original = Stripe.getApiBase();
        server.start();
        Stripe.overrideApiBase("http://127.0.0.1:" + server.getAddress().getPort());
        try {
            adapter.reverseTransfer(id, "tr_fixture");
            assertEquals(List.of("GET /v1/transfers/tr_fixture"), calls);
        } finally {
            Stripe.overrideApiBase(original);
            server.stop(0);
        }
    }
}
