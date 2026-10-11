package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.PaymentOrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPaymentOrderRepository extends JpaRepository<PaymentOrderEntity, UUID> {

    Optional<PaymentOrderEntity> findByGatewayOrderId(String gatewayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentOrderEntity p WHERE p.id = :id")
    Optional<PaymentOrderEntity> findByIdWithLock(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentOrderEntity p WHERE p.gatewayOrderId = :gatewayOrderId")
    Optional<PaymentOrderEntity> findByGatewayOrderIdWithLock(@Param("gatewayOrderId") String gatewayOrderId);

    List<PaymentOrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<PaymentOrderEntity> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
}
