package com.tyson.digitalwallet.ddd.application.port.payment.result;

public record PaymentLinkResult(
        String gatewayOrderId,
        String checkoutUrl
) {}
