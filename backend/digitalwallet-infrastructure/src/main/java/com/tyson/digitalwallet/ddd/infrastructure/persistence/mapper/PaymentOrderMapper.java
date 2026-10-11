package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.PaymentOrderEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.TransactionEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.UserEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.WalletEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentOrderMapper {

    public PaymentOrderEntity toEntity(PaymentOrder domain) {
        if (domain == null) {
            return null;
        }

        WalletEntity wallet = new WalletEntity();
        wallet.setId(domain.getWalletId());

        UserEntity user = new UserEntity();
        user.setId(domain.getUserId());

        TransactionEntity transaction = null;
        if (domain.getTransactionId() != null) {
            transaction = new TransactionEntity();
            transaction.setId(domain.getTransactionId());
        }

        return new PaymentOrderEntity(
                domain.getId(),
                wallet,
                user,
                transaction,
                domain.getAmount(),
                domain.getGateway(),
                domain.getGatewayOrderId(),
                domain.getStatus(),
                domain.getCheckoutUrl(),
                domain.getCreatedAt(),
                domain.getCompletedAt()
        );
    }

    public PaymentOrder toDomain(PaymentOrderEntity entity) {
        if (entity == null) {
            return null;
        }

        return new PaymentOrder(
                entity.getId(),
                entity.getWallet() != null ? entity.getWallet().getId() : null,
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getTransaction() != null ? entity.getTransaction().getId() : null,
                entity.getAmount(),
                entity.getGateway(),
                entity.getGatewayOrderId(),
                entity.getStatus(),
                entity.getCheckoutUrl(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}
