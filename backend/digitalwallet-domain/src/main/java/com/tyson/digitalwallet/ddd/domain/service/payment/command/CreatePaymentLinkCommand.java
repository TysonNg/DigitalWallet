package com.tyson.digitalwallet.ddd.domain.service.payment.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentLinkCommand(
        UUID orderId,
        BigDecimal amount,
        String description,
        String returnUrl,
        String cancelUrl
) {}
