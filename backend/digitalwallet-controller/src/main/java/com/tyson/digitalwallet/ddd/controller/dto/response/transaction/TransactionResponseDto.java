package com.tyson.digitalwallet.ddd.controller.dto.response.transaction;

import com.tyson.digitalwallet.ddd.application.usecase.transaction.response.TransactionResponse;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponseDto(
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
    public static TransactionResponseDto from(TransactionResponse response) {
        if (response == null) {
            return null;
        }
        return new TransactionResponseDto(
                response.id(),
                response.type(),
                response.amount(),
                response.amountBefore(),
                response.amountAfter(),
                response.status(),
                response.description(),
                response.senderWalletId(),
                response.receiverWalletId(),
                response.createdAt()
        );
    }
}
