package com.tyson.digitalwallet.ddd.infrastructure.persistence.adapter.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.service.payment.command.CreatePaymentLinkCommand;
import com.tyson.digitalwallet.ddd.domain.service.payment.port.PaymentGatewayPort;
import com.tyson.digitalwallet.ddd.domain.service.payment.result.PaymentLinkResult;
import com.tyson.digitalwallet.ddd.domain.service.payment.result.PaymentWebhookResult;
import com.tyson.digitalwallet.ddd.domain.exception.PaymentGatewayUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PayOsPaymentAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(PayOsPaymentAdapter.class);

    private final String clientId;
    private final String apiKey;
    private final String checksumKey;
    private final ObjectMapper objectMapper;
    private PayOS payOS;

    public PayOsPaymentAdapter(
            @Value("${payment.payos.client-id:}") String clientId,
            @Value("${payment.payos.api-key:}") String apiKey,
            @Value("${payment.payos.checksum-key:}") String checksumKey,
            ObjectMapper objectMapper
    ) {
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.checksumKey = checksumKey;
        this.objectMapper = objectMapper;

        if (clientId != null && !clientId.isBlank() && apiKey != null && !apiKey.isBlank()) {
            this.payOS = new PayOS(clientId, apiKey, checksumKey);
        } else {
            log.warn("PayOS credentials are not configured. VietQR gateway will be unavailable until credentials are provided.");
        }
    }

    @Override
    public PaymentGatewayType getGatewayType() {
        return PaymentGatewayType.PAYOS;
    }

    @Override
    @CircuitBreaker(name = "payosPayment", fallbackMethod = "createPaymentLinkFallback")
    public PaymentLinkResult createPaymentLink(CreatePaymentLinkCommand command) {
        if (payOS == null) {
            throw new IllegalStateException("PayOS credentials are not configured in application properties.");
        }

        try {
            // PayOS yêu cầu orderCode là số nguyên (tối đa 9007199254740991)
            // Sinh orderCode từ timestamp + 4 chữ số ngẫu nhiên
            long orderCode = System.currentTimeMillis() % 1000000000000L + (long) (Math.random() * 1000);

            long amountLong = command.amount().longValueExact();
            String desc = command.description();
            if (desc == null || desc.isBlank()) {
                desc = "Nap tien vi " + orderCode;
            }
            // PayOS giới hạn độ dài description tối đa 25 ký tự
            if (desc.length() > 25) {
                desc = desc.substring(0, 25);
            }

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amountLong)
                    .description(desc)
                    .returnUrl(command.returnUrl())
                    .cancelUrl(command.cancelUrl())
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);

            return new PaymentLinkResult(
                    String.valueOf(response.getOrderCode()),
                    response.getCheckoutUrl()
            );
        } catch (Exception e) {
            log.error("Failed to create PayOS payment link: {}", e.getMessage(), e);
            throw new RuntimeException("Could not create PayOS payment link: " + e.getMessage(), e);
        }
    }

    public PaymentLinkResult createPaymentLinkFallback(CreatePaymentLinkCommand command, Throwable t) {
        log.error("PayOS circuit breaker fallback triggered for order [{}]: {}", command.orderId(), t.getMessage());
        if (t instanceof PaymentGatewayUnavailableException) {
            throw (PaymentGatewayUnavailableException) t;
        }
        throw new PaymentGatewayUnavailableException(
                "Cổng thanh toán VietQR (PayOS) tạm thời gián đoạn. Vui lòng thử lại sau ít phút hoặc sử dụng phương thức thanh toán thẻ quốc tế.",
                t
        );
    }

    @Override
    public PaymentWebhookResult verifyWebhook(String rawPayload, Map<String, String> headers) {
        if (payOS == null) {
            return PaymentWebhookResult.invalid("PayOS not configured");
        }

        try {
            Webhook webhook = objectMapper.readValue(rawPayload, Webhook.class);
            WebhookData data = payOS.webhooks().verify(webhook);

            if (data == null) {
                return PaymentWebhookResult.invalid("PayOS webhook verification returned null");
            }

            String orderCode = String.valueOf(data.getOrderCode());
            BigDecimal amount = BigDecimal.valueOf(data.getAmount());

            // Mã "00" thể hiện giao dịch thanh toán thành công
            if ("00".equals(data.getCode())) {
                return PaymentWebhookResult.success(orderCode, amount, data.getCurrency());
            } else {
                return PaymentWebhookResult.failed(orderCode, "PayOS returned code: " + data.getCode() + " (" + data.getDesc() + ")");
            }
        } catch (Exception e) {
            log.error("Failed to verify PayOS webhook: {}", e.getMessage(), e);
            return PaymentWebhookResult.invalid("Invalid PayOS webhook signature or payload: " + e.getMessage());
        }
    }
}
