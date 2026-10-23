package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.PayoutAttempt;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.PayoutAttemptEntity;

import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class PayoutAttemptMapper {
    public PayoutAttemptEntity toEntity(PayoutAttempt domain, PayoutAttemptEntity entity) {
        entity.setId(domain.getId());
        entity.setWithdrawalId(domain.getWithdrawalId());
        entity.setProvider(domain.getProvider());
        entity.setAmount(domain.getAmount());
        entity.setCurrency(domain.getCurrency());
        entity.setStatus(domain.getStatus());
        entity.setError(domain.getError());
        entity.setMetadata(new HashMap<>(domain.getMetadata()));
        entity.setProviderReference(domain.getMetadata().get("payout_reference"));
        entity.setProviderAccountReference(domain.getMetadata().get("account_reference"));
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public PayoutAttempt toDomain(PayoutAttemptEntity entity) {
        return new PayoutAttempt(
                entity.getId(),
                entity.getWithdrawalId(),
                entity.getProvider(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getMetadata(),
                entity.getCreatedAt(),
                entity.getStatus(),
                entity.getError());
    }
}
