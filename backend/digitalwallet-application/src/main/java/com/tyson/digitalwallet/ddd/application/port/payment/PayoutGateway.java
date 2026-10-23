package com.tyson.digitalwallet.ddd.application.port.payment;

import com.tyson.digitalwallet.ddd.domain.model.entity.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

public interface PayoutGateway {
    String provider();

    Quote validate(BigDecimal amount, String currency, String destination);

    Result execute(
            WithdrawalRequest request, PayoutAttempt attempt, Consumer<PayoutAttempt> checkpoint);

    void compensate(WithdrawalRequest request, PayoutAttempt attempt);

    Notice verifyWebhook(String body, String signature);

    record Quote(long amount, String currency, int currencyExponent) {}

    enum Outcome {
        PENDING,
        PAID,
        FAILED,
        REVIEW_REQUIRED
    }

    record Result(Outcome outcome, String detail) {}

    record Notice(
            String eventId,
            UUID withdrawalId,
            String accountReference,
            String payoutReference,
            long amount,
            String currency) {}
}
