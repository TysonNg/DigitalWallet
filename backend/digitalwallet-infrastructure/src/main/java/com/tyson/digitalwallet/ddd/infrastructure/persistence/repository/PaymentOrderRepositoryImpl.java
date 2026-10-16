package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;
import com.tyson.digitalwallet.ddd.domain.repository.PaymentOrderRepository;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.PaymentOrderMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.PaymentOrderEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentOrderRepositoryImpl implements PaymentOrderRepository {

    private final SpringDataPaymentOrderRepository springDataRepository;
    private final PaymentOrderMapper mapper;

    public PaymentOrderRepositoryImpl(
            SpringDataPaymentOrderRepository springDataRepository,
            PaymentOrderMapper mapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public PaymentOrder save(PaymentOrder paymentOrder) {
        PaymentOrderEntity entity = mapper.toEntity(paymentOrder);
        PaymentOrderEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PaymentOrder> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentOrder> findByIdWithLock(UUID id) {
        return springDataRepository.findByIdWithLock(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentOrder> findByGatewayOrderId(String gatewayOrderId) {
        return springDataRepository.findByGatewayOrderId(gatewayOrderId).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentOrder> findByGatewayOrderIdWithLock(String gatewayOrderId) {
        return springDataRepository.findByGatewayOrderIdWithLock(gatewayOrderId).map(mapper::toDomain);
    }

    @Override
    public List<PaymentOrder> findByUserId(UUID userId) {
        return springDataRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PaymentOrder> findByWalletId(UUID walletId) {
        return springDataRepository.findByWalletIdOrderByCreatedAtDesc(walletId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PaymentOrder> findPendingOrdersCreatedBefore(LocalDateTime threshold) {
        return springDataRepository.findByStatusAndCreatedAtBefore(PaymentOrderStatus.PENDING, threshold).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
