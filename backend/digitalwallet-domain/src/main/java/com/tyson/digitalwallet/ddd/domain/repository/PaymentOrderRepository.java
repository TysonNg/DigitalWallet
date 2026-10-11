package com.tyson.digitalwallet.ddd.domain.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository {
    PaymentOrder save(PaymentOrder paymentOrder);

    Optional<PaymentOrder> findById(UUID id);

    Optional<PaymentOrder> findByIdWithLock(UUID id);

    Optional<PaymentOrder> findByGatewayOrderId(String gatewayOrderId);

    Optional<PaymentOrder> findByGatewayOrderIdWithLock(String gatewayOrderId);

    List<PaymentOrder> findByUserId(UUID userId);

    List<PaymentOrder> findByWalletId(UUID walletId);
}
