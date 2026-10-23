package com.tyson.digitalwallet.ddd.controller.dto.response.wallet;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WithdrawalResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WithdrawalResponseDto(
        UUID id,
        UUID walletId,
        BigDecimal amount,
        BigDecimal payoutAmount,
        String payoutCurrency,
        String mode,
        String testBank,
        String status,
        String stripeAccountId,
        String transferId,
        String payoutId,
        String error,
        Instant createdAt,
        Instant updatedAt,
        String provider,
        String currency,
        String destination) {
    public static WithdrawalResponseDto from(WithdrawalResponse r) {
        boolean stripe = "STRIPE".equals(r.provider());
        // The gateway quote carries the payout currency exponent; source and payout amounts are
        // independent.
        return new WithdrawalResponseDto(
                r.id(),
                r.walletId(),
                r.amount(),
                BigDecimal.valueOf(r.payoutMinorAmount(), r.payoutCurrencyExponent()),
                r.payoutCurrency(),
                stripe ? "STRIPE_TEST" : null,
                r.destination().startsWith("test-bank:") ? r.destination().substring(10) : null,
                r.status().name(),
                stripe ? r.accountReference() : null,
                r.transferReference(),
                r.payoutReference(),
                r.error(),
                r.createdAt(),
                r.updatedAt(),
                r.provider(),
                r.currency(),
                r.destination());
    }
}
