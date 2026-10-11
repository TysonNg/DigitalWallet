package com.tyson.digitalwallet.ddd.domain.service.payment.result;

public record PaymentLinkResult(
        String gatewayOrderId,
        String checkoutUrl
) {}
