package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.IdempotencyKey;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.IdempotencyKeyEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.TransactionEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyMapper {

    public IdempotencyKeyEntity toEntity(IdempotencyKey domain) {
        if (domain == null) {
            return null;
        }

        UserEntity userEntity = null;
        if (domain.getUserId() != null) {
            userEntity = new UserEntity();
            userEntity.setId(domain.getUserId());
        }

        TransactionEntity transactionEntity = null;
        if (domain.getTransactionId() != null) {
            transactionEntity = new TransactionEntity();
            transactionEntity.setId(domain.getTransactionId());
        }

        return new IdempotencyKeyEntity(
                domain.getId(),
                userEntity,
                domain.getIdempotencyKey(),
                transactionEntity,
                domain.getRequestHash(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getExpiresAt()
        );
    }

    public IdempotencyKey toDomain(IdempotencyKeyEntity entity) {
        if (entity == null) {
            return null;
        }

        return new IdempotencyKey(
                entity.getId(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getIdempotencyKey(),
                entity.getTransaction() != null ? entity.getTransaction().getId() : null,
                entity.getRequestHash(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getExpiresAt()
        );
    }
}
