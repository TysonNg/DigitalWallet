package com.tyson.digitalwallet.ddd.controller.dto.request.payment;

import com.tyson.digitalwallet.ddd.application.usecase.payment.command.CreatePaymentOrderCommand;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentOrderRequestDto(
        UUID walletId,

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1000", message = "Amount must be at least 1,000 VND")
        BigDecimal amount,

        @NotNull(message = "Payment gateway type is required (PAYOS or STRIPE)")
        PaymentGatewayType gateway,

        String returnUrl,
        String cancelUrl
) {
    public CreatePaymentOrderCommand toCommand(String defaultReturnUrl, String defaultCancelUrl) {
        String finalReturnUrl = (returnUrl != null && !returnUrl.isBlank()) ? returnUrl : defaultReturnUrl;
        String finalCancelUrl = (cancelUrl != null && !cancelUrl.isBlank()) ? cancelUrl : defaultCancelUrl;
        return new CreatePaymentOrderCommand(walletId, userId, amount, gateway, finalReturnUrl, finalCancelUrl);
    }
}
