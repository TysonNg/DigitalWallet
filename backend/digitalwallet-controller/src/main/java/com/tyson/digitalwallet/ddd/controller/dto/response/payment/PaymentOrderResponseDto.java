package com.tyson.digitalwallet.ddd.controller.dto.response.payment;

import com.tyson.digitalwallet.ddd.application.usecase.payment.response.PaymentOrderResponse;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentOrderResponseDto(
        UUID id,
        UUID walletId,
        UUID userId,
        UUID transactionId,
        BigDecimal amount,
        PaymentGatewayType gateway,
        String gatewayOrderId,
        PaymentOrderStatus status,
        String checkoutUrl,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static PaymentOrderResponseDto from(PaymentOrderResponse r) {
        if (r == null) {
            return null;
        }
        return new PaymentOrderResponseDto(
                r.id(),
                r.walletId(),
                r.userId(),
                r.transactionId(),
                r.amount(),
                r.gateway(),
                r.gatewayOrderId(),
                r.status(),
                r.checkoutUrl(),
                r.createdAt(),
                r.completedAt()
        );
    }
}
