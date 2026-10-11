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
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session == null) {
                    return PaymentWebhookResult.invalid("Unable to deserialize Stripe checkout session");
                }

                BigDecimal amount = session.getAmountTotal() != null
                        ? BigDecimal.valueOf(session.getAmountTotal())
                        : BigDecimal.ZERO;

                return PaymentWebhookResult.success(session.getId(), amount);
            }

            return PaymentWebhookResult.failed(null, "Ignored unhandled Stripe event type: " + event.getType());
        } catch (Exception e) {
            log.error("Failed to verify Stripe webhook: {}", e.getMessage(), e);
            return PaymentWebhookResult.invalid("Invalid Stripe webhook signature: " + e.getMessage());
        }
    }
}
