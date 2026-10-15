package com.tyson.digitalwallet.ddd.application.port.payment.result;

import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;
import java.math.BigDecimal;

public record PaymentWebhookResult(
        boolean isValid,
        String gatewayOrderId,
        PaymentOrderStatus status,
        BigDecimal amount,
        String currency,
        String message
) {
    public static PaymentWebhookResult invalid(String message) {
        return new PaymentWebhookResult(false, null, null, null, null, message);
    }

    public static PaymentWebhookResult success(String gatewayOrderId, BigDecimal amount, String currency) {
        return new PaymentWebhookResult(true, gatewayOrderId, PaymentOrderStatus.SUCCESS, amount, currency, "Success");
    }

    public static PaymentWebhookResult pending(String gatewayOrderId, BigDecimal amount, String currency) {
        return new PaymentWebhookResult(true, gatewayOrderId, PaymentOrderStatus.PENDING, amount, currency, "Payment is not yet paid");
    }

    public static PaymentWebhookResult failed(String gatewayOrderId, String message) {
        return new PaymentWebhookResult(true, gatewayOrderId, PaymentOrderStatus.FAILED, null, null, message);
    }

    public static PaymentWebhookResult cancelled(String gatewayOrderId, String message) {
        return new PaymentWebhookResult(true, gatewayOrderId, PaymentOrderStatus.CANCELLED, null, null, message);
    }
}