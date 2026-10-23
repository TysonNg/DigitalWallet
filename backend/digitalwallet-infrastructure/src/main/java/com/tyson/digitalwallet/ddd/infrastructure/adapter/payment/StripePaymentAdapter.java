package com.tyson.digitalwallet.ddd.infrastructure.adapter.payment;

import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.tyson.digitalwallet.ddd.application.port.payment.PaymentGatewayPort;
import com.tyson.digitalwallet.ddd.application.port.payment.command.CreatePaymentLinkCommand;
import com.tyson.digitalwallet.ddd.application.port.payment.result.PaymentLinkResult;
import com.tyson.digitalwallet.ddd.application.port.payment.result.PaymentWebhookResult;
import com.tyson.digitalwallet.ddd.domain.exception.PaymentGatewayUnavailableException;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class StripePaymentAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentAdapter.class);

    private final String secretKey;
    private final String webhookSecret;

    public StripePaymentAdapter(
            @Value("${payment.stripe.secret-key:}") String secretKey,
            @Value("${payment.stripe.webhook-secret:}") String webhookSecret) {
        if (secretKey != null
                && !secretKey.isBlank()
                && !secretKey.startsWith("sk_test_")
                && !secretKey.startsWith("rk_test_")) {
            throw new IllegalArgumentException("Only Stripe sandbox credentials are allowed");
        }
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;

        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
        } else {
            log.warn(
                    "Stripe secret key is not configured. Stripe card payment gateway will be"
                        + " unavailable.");
        }
    }

    @Override
    public PaymentGatewayType getGatewayType() {
        return PaymentGatewayType.STRIPE;
    }

    @Override
    @CircuitBreaker(name = "stripePayment", fallbackMethod = "createPaymentLinkFallback")
    public PaymentLinkResult createPaymentLink(CreatePaymentLinkCommand command) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "Stripe secret key is not configured in application properties.");
        }

        try {
            Stripe.apiKey = secretKey;

            String desc = command.description();
            if (desc == null || desc.isBlank()) {
                desc = "Nap tien vi Digital Wallet";
            }

            // VND trong Stripe là zero-decimal currency (1 VND = 1 đơn vị)
            long amountLong = command.amount().longValueExact();

            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl(command.returnUrl())
                            .setCancelUrl(command.cancelUrl())
                            .setClientReferenceId(command.orderId().toString())
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setQuantity(1L)
                                            .setPriceData(
                                                    SessionCreateParams.LineItem.PriceData.builder()
                                                            .setCurrency("vnd")
                                                            .setUnitAmount(amountLong)
                                                            .setProductData(
                                                                    SessionCreateParams.LineItem
                                                                            .PriceData.ProductData
                                                                            .builder()
                                                                            .setName(desc)
                                                                            .build())
                                                            .build())
                                            .build())
                            .build();

            Session session = Session.create(params);

            return new PaymentLinkResult(session.getId(), session.getUrl());
        } catch (Exception e) {
            log.error("Failed to create Stripe checkout session: {}", e.getMessage(), e);
            throw new RuntimeException(
                    "Could not create Stripe checkout session: " + e.getMessage(), e);
        }
    }

    public PaymentLinkResult createPaymentLinkFallback(
            CreatePaymentLinkCommand command, Throwable t) {
        log.error(
                "Stripe circuit breaker fallback triggered for order [{}]: {}",
                command.orderId(),
                t.getMessage());
        if (t instanceof PaymentGatewayUnavailableException) {
            throw (PaymentGatewayUnavailableException) t;
        }
        throw new PaymentGatewayUnavailableException(
                "Cổng thanh toán thẻ quốc tế (Stripe) tạm thời gián đoạn. Vui lòng thử lại sau ít"
                    + " phút hoặc chọn phương thức VietQR.",
                t);
    }

    @Override
    public PaymentWebhookResult verifyWebhook(String rawPayload, Map<String, String> headers) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return PaymentWebhookResult.invalid("Stripe webhook secret is not configured");
        }

        try {
            String sigHeader = null;
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if ("stripe-signature".equalsIgnoreCase(entry.getKey())) {
                    sigHeader = entry.getValue();
                    break;
                }
            }

            if (sigHeader == null || sigHeader.isBlank()) {
                return PaymentWebhookResult.invalid("Missing Stripe-Signature header");
            }

            Event event = Webhook.constructEvent(rawPayload, sigHeader, webhookSecret);
            if (!Boolean.FALSE.equals(event.getLivemode())) {
                return PaymentWebhookResult.invalid(
                        "Live Stripe events are forbidden in sandbox mode");
            }

            if ("checkout.session.completed".equals(event.getType())
                    || "checkout.session.async_payment_succeeded".equals(event.getType())) {
                // Parse only after signature verification. This also handles SDK/API version
                // differences.
                var session =
                        new com.fasterxml.jackson.databind.ObjectMapper()
                                .readTree(rawPayload)
                                .path("data")
                                .path("object");
                var id = session.path("id");
                var total = session.path("amount_total");
                var currencyNode = session.path("currency");
                var paymentStatus = session.path("payment_status");
                if (!"checkout.session".equals(session.path("object").asText())
                        || !id.isTextual()
                        || id.asText().isBlank()
                        || !total.isIntegralNumber()
                        || !total.canConvertToLong()
                        || total.longValue() <= 0
                        || !currencyNode.isTextual()
                        || currencyNode.asText().isBlank()) {
                    return PaymentWebhookResult.invalid(
                            "Stripe checkout session is missing valid id, amount or currency");
                }
                String sessionId = id.asText();
                BigDecimal amount = BigDecimal.valueOf(total.longValue());
                String currency = currencyNode.asText();
                if ("unpaid".equals(paymentStatus.asText())) {
                    return PaymentWebhookResult.pending(sessionId, amount, currency);
                }
                if (!"paid".equals(paymentStatus.asText())) {
                    return PaymentWebhookResult.invalid(
                            "Stripe checkout payment_status must be paid to fund a wallet");
                }
                log.info(
                        "Verified paid Stripe checkout session: {}, amount: {}, currency: {}",
                        sessionId,
                        amount,
                        currency);
                return PaymentWebhookResult.success(sessionId, amount, currency);
            }
            return PaymentWebhookResult.failed(
                    null, "Ignored unhandled Stripe event type: " + event.getType());
        } catch (Exception e) {
            log.error("Failed to verify Stripe webhook: {}", e.getMessage(), e);
            return PaymentWebhookResult.invalid(
                    "Invalid Stripe webhook signature: " + e.getMessage());
        }
    }

    @Override
    public PaymentOrderStatus queryOrderStatus(String gatewayOrderId) {
        if (secretKey == null
                || secretKey.isBlank()
                || gatewayOrderId == null
                || gatewayOrderId.isBlank()) {
            return PaymentOrderStatus.PENDING;
        }

        try {
            Session session = Session.retrieve(gatewayOrderId);
            if (session == null) {
                return PaymentOrderStatus.PENDING;
            }

            if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                return PaymentOrderStatus.SUCCESS;
            }
            if ("expired".equalsIgnoreCase(session.getStatus())) {
                return PaymentOrderStatus.EXPIRED;
            }
            return PaymentOrderStatus.PENDING;
        } catch (Exception e) {
            log.error(
                    "Failed to query Stripe checkout session for gatewayOrderId {}: {}",
                    gatewayOrderId,
                    e.getMessage(),
                    e);
            return PaymentOrderStatus.PENDING;
        }
    }
}
