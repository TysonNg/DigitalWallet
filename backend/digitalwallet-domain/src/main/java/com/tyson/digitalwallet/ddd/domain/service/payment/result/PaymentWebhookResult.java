package com.tyson.digitalwallet.ddd.domain.service.payment.result;

import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;

import java.math.BigDecimal;

public record PaymentWebhookResult(
        boolean isValid,
        String gatewayOrderId,
        PaymentOrderStatus status,
        BigDecimal amount,
        String message
) {
    public static PaymentWebhookResult invalid(String message) {
        return new PaymentWebhookResult(false, null, null, null, message);
    }

    public static PaymentWebhookResult success(String gatewayOrderId, BigDecimal amount) {
        return new PaymentWebhookResult(true, gatewayOrderId, PaymentOrderStatus.SUCCESS, amount, "Success");
    }

    public static PaymentWebhookResult failed(String gatewayOrderId, String message) {
        return new PaymentWebhookResult(true, gatewayOrderId, PaymentOrderStatus.FAILED, null, message);
    }

    public static PaymentWebhookResult cancelled(String gatewayOrderId, String message) {
        return new PaymentWebhookResult(true, gatewayOrderId, PaymentOrderStatus.CANCELLED, null, message);
    }
}
