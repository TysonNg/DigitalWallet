package com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper;

import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.TransactionEntity;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.WalletEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionEntity toEntity(Transaction domain) {
        if (domain == null) {
            return null;
        }

        WalletEntity senderWallet = null;
        if (domain.getSenderWalletId() != null) {
            senderWallet = new WalletEntity();
            senderWallet.setId(domain.getSenderWalletId());
        }

        WalletEntity receiverWallet = null;
        if (domain.getReceiverWalletId() != null) {
            receiverWallet = new WalletEntity();
            receiverWallet.setId(domain.getReceiverWalletId());
        }

        return new TransactionEntity(
                domain.getId(),
                domain.getType(),
                domain.getAmount(),
                domain.getAmountBefore(),
                domain.getAmountAfter(),
                domain.getStatus(),
                domain.getDescription(),
                receiverWallet,
                senderWallet,
                domain.getCreatedAt()
        );
    }

    public Transaction toDomain(TransactionEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Transaction(
                entity.getId(),
                entity.getType(),
                entity.getAmount(),
                entity.getAmountBefore(),
                entity.getAmountAfter(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getSenderWallet() != null ? entity.getSenderWallet().getId() : null,
                entity.getReceiverWallet() != null ? entity.getReceiverWallet().getId() : null,
                entity.getCreatedAt()
        );
    }
}
