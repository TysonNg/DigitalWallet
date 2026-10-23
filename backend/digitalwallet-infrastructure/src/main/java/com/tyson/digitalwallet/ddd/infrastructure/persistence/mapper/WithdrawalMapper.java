package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.WithdrawalRequest;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.WithdrawalRequestEntity;

import org.springframework.stereotype.Component;

@Component
public class WithdrawalMapper {
    public WithdrawalRequestEntity toEntity(WithdrawalRequest domain) {
        if (domain == null) return null;
        var entity = new WithdrawalRequestEntity();
        entity.setId(domain.getId());
        entity.setWalletId(domain.getWalletId());
        entity.setUserId(domain.getUserId());
        entity.setAmount(domain.getAmount());
        entity.setBalanceBefore(domain.getBalanceBefore());
        entity.setCurrency(domain.getCurrency());
        entity.setDestination(domain.getDestination());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setStatus(domain.getStatus());
        entity.setTransactionId(domain.getTransactionId());
        entity.setError(domain.getError());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public WithdrawalRequest toDomain(WithdrawalRequestEntity entity) {
        if (entity == null) return null;
        return new WithdrawalRequest(
                entity.getId(),
                entity.getWalletId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getBalanceBefore(),
                entity.getCurrency(),
                entity.getDestination(),
                entity.getIdempotencyKey(),
                entity.getStatus(),
                entity.getTransactionId(),
                entity.getError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
