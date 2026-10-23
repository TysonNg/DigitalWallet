package com.tyson.digitalwallet.ddd.application.usecase.wallet.response;

import com.tyson.digitalwallet.ddd.domain.model.entity.*;
import com.tyson.digitalwallet.ddd.domain.model.enums.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WithdrawalResponse(
        UUID id,
        UUID walletId,
        BigDecimal amount,
        String currency,
        String destination,
        WithdrawalStatus status,
        String provider,
        long payoutMinorAmount,
        String payoutCurrency,
        int payoutCurrencyExponent,
        String accountReference,
        String transferReference,
        String payoutReference,
        String error,
        Instant createdAt,
        Instant updatedAt) {
    public static WithdrawalResponse from(WithdrawalRequest r, PayoutAttempt a) {
        return new WithdrawalResponse(
                r.getId(),
                r.getWalletId(),
                r.getAmount(),
                r.getCurrency(),
                r.getDestination(),
                r.getStatus(),
                a.getProvider(),
                a.getAmount(),
                a.getCurrency(),
                Integer.parseInt(a.getMetadata().getOrDefault("currency_exponent", "2")),
                a.getMetadata().get("account_reference"),
                a.getMetadata().get("transfer_reference"),
                a.getMetadata().get("payout_reference"),
                r.getError(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
