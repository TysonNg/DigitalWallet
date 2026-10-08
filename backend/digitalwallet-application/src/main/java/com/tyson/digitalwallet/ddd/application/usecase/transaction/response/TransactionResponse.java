package com.tyson.digitalwallet.ddd.application.usecase.transaction.response;

import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal amountBefore,
        BigDecimal amountAfter,
        TransactionStatus status,
        String description,
        UUID senderWalletId,
        UUID receiverWalletId,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getAmountBefore(),
                transaction.getAmountAfter(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getSenderWalletId(),
                transaction.getReceiverWalletId(),
                transaction.getCreatedAt()
        );
    }
}
