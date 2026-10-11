package com.tyson.digitalwallet.ddd.application.usecase.payment.command;

import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentOrderCommand(
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        PaymentGatewayType gateway,
        String returnUrl,
        String cancelUrl
) {
}
