package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.Notification;
import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.NotificationEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.TransactionEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationEntity toEntity(Notification domain) {
        if(domain == null) {
            return null;
        }

        UserEntity user = new UserEntity();
        user.setId(domain.getUserId());

        TransactionEntity transaction = null;
        if(domain.getTransactionId() != null) {
            transaction = new TransactionEntity();
            transaction.setId(domain.getTransactionId());
        }

        return new NotificationEntity(
                domain.getId(),
                user,
                domain.getTitle(),
                domain.getContent(),
                transaction,
                domain.getType(),
                domain.isRead(),
                domain.getReadAt(),
                domain.getCreatedAt()
        );
    }

    public Notification toDomain(NotificationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Notification(
                entity.getId(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getTitle(),
                entity.getContent(),
                entity.getTransaction() != null ? entity.getTransaction().getId() : null,
                entity.getType(),
                entity.isRead(),
                entity.getReadAt(),
                entity.getCreatedAt()
        );
    }
}
