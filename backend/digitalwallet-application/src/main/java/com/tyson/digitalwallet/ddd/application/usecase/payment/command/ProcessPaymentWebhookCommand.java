package com.tyson.digitalwallet.ddd.application.usecase.payment.command;

import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;

import java.util.Map;

public record ProcessPaymentWebhookCommand(
        PaymentGatewayType gateway,
        String rawPayload,
        Map<String, String> headers
) {
}
