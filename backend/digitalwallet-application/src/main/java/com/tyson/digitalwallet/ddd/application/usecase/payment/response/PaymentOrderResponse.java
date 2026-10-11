package com.tyson.digitalwallet.ddd.application.usecase.payment.response;

import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentOrderResponse(
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
    public static PaymentOrderResponse from(PaymentOrder order) {
        if (order == null) {
            return null;
        }
        return new PaymentOrderResponse(
                order.getId(),
                order.getWalletId(),
                order.getUserId(),
                order.getTransactionId(),
                order.getAmount(),
                order.getGateway(),
                order.getGatewayOrderId(),
                order.getStatus(),
                order.getCheckoutUrl(),
                order.getCreatedAt(),
                order.getCompletedAt()
        );
    }
}
