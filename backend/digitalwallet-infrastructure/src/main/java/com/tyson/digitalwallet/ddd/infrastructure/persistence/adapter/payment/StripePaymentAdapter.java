package com.tyson.digitalwallet.ddd.infrastructure.persistence.adapter.payment;

import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.service.payment.command.CreatePaymentLinkCommand;
import com.tyson.digitalwallet.ddd.domain.service.payment.port.PaymentGatewayPort;
import com.tyson.digitalwallet.ddd.domain.service.payment.result.PaymentLinkResult;
import com.tyson.digitalwallet.ddd.domain.service.payment.result.PaymentWebhookResult;
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
            @Value("${payment.stripe.webhook-secret:}") String webhookSecret
    ) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;

        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
        } else {
            log.warn("Stripe secret key is not configured. Stripe card payment gateway will be unavailable.");
        }
    }

    @Override
    public PaymentGatewayType getGatewayType() {
        return PaymentGatewayType.STRIPE;
    }

    @Override
    public PaymentLinkResult createPaymentLink(CreatePaymentLinkCommand command) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key is not configured in application properties.");
        }

        try {
            Stripe.apiKey = secretKey;

            String desc = command.description();
            if (desc == null || desc.isBlank()) {
                desc = "Nap tien vi Digital Wallet";
            }

            // VND trong Stripe là zero-decimal currency (1 VND = 1 đơn vị)
            long amountLong = command.amount().longValue();

            SessionCreateParams params = SessionCreateParams.builder()
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
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(desc)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            return new PaymentLinkResult(
                    session.getId(),
                    session.getUrl()
            );
        } catch (Exception e) {
            log.error("Failed to create Stripe checkout session: {}", e.getMessage(), e);
            throw new RuntimeException("Could not create Stripe checkout session: " + e.getMessage(), e);
        }
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

            if ("checkout.session.completed".equals(event.getType())) {
                String sessionId = null;
                BigDecimal amount = BigDecimal.ZERO;

                var deserializer = event.getDataObjectDeserializer();
                Session session = null;
                if (deserializer.getObject().isPresent() && deserializer.getObject().get() instanceof Session s) {
                    session = s;
                } else {
                    try {
                        var unsafeObj = deserializer.deserializeUnsafe();
                        if (unsafeObj instanceof Session s) {
                            session = s;
                        }
                    } catch (Exception ex) {
                        log.warn("Stripe deserializeUnsafe failed: {}", ex.getMessage());
                    }
                }

                if (session != null) {
                    sessionId = session.getId();
                    if (session.getAmountTotal() != null) {
                        amount = BigDecimal.valueOf(session.getAmountTotal());
                    }
                }

                // Fallback: Parse raw JSON directly using Jackson if session or sessionId is null
                if (sessionId == null || sessionId.isBlank()) {
                    try {
                        com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(rawPayload);
                        com.fasterxml.jackson.databind.JsonNode objNode = rootNode.path("data").path("object");
                        sessionId = objNode.path("id").asText(null);
                        long amountTotal = objNode.path("amount_total").asLong(0L);
                        amount = BigDecimal.valueOf(amountTotal);
                    } catch (Exception jsonEx) {
                        log.error("Failed to parse raw JSON for Stripe checkout session: {}", jsonEx.getMessage());
                    }
                }

                if (sessionId == null || sessionId.isBlank()) {
                    return PaymentWebhookResult.invalid("Unable to deserialize Stripe checkout session");
                }

                log.info("Successfully extracted Stripe checkout session: {}, amount: {}", sessionId, amount);
                return PaymentWebhookResult.success(sessionId, amount);
            }

            return PaymentWebhookResult.failed(null, "Ignored unhandled Stripe event type: " + event.getType());
        } catch (Exception e) {
            log.error("Failed to verify Stripe webhook: {}", e.getMessage(), e);
            return PaymentWebhookResult.invalid("Invalid Stripe webhook signature: " + e.getMessage());
        }
    }
}
