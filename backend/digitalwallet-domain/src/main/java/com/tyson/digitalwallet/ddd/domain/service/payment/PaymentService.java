package com.tyson.digitalwallet.ddd.domain.service.payment;

import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
    PaymentOrder initializeOrder(UUID walletId, UUID userId, BigDecimal amount, PaymentGatewayType gateway);

    PaymentOrder updateCheckoutInfo(UUID orderId, String gatewayOrderId, String checkoutUrl);

    PaymentOrder completeOrder(UUID orderId, UUID transactionId);

    PaymentOrder failOrder(UUID orderId);

    PaymentOrder cancelOrder(UUID orderId);

    PaymentOrder expireOrder(UUID orderId);
}
