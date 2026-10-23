package com.tyson.digitalwallet.ddd.infrastructure.adapter.payment;

import com.tyson.digitalwallet.ddd.application.port.payment.*;
import com.tyson.digitalwallet.ddd.domain.model.entity.*;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/** Translates the Stripe Connect workflow into the common payout contract. */
@Component
public class StripePayoutGateway implements PayoutGateway {
    private final StripePayoutPort stripe;

    public StripePayoutGateway(StripePayoutPort stripe) {
        this.stripe = stripe;
    }

    @Override
    public String provider() {
        return "STRIPE";
    }

    @Override
    public Quote validate(BigDecimal amount, String currency, String destination) {
        stripe.assertTestConfiguration();
        if (!"VND".equals(currency)
                || amount == null
                || amount.compareTo(BigDecimal.valueOf(25000)) < 0
                || amount.compareTo(BigDecimal.valueOf(100000000)) > 0
                || amount.remainder(BigDecimal.valueOf(250)).signum() != 0
                || !Set.of("test-bank:SUCCESS", "test-bank:FAILURE")
                        .contains(destination == null ? "" : destination))
            throw new IllegalArgumentException(
                    "Stripe sandbox requires a test bank and VND 25,000-100,000,000 in multiples of"
                            + " 250");
        return new Quote(amount.divide(BigDecimal.valueOf(250)).longValueExact(), "USD", 2);
    }

    @Override
    public Result execute(
            WithdrawalRequest request, PayoutAttempt initial, Consumer<PayoutAttempt> checkpoint) {
        var a = initial;
        boolean replayingUnknownOperation = false;
        if (a.getMetadata().get("payout_reference") == null
                && a.getCreatedAt().isBefore(Instant.now().minusSeconds(23 * 3600)))
            return new Result(
                    Outcome.REVIEW_REQUIRED,
                    "Provider idempotency window requires manual reconciliation");
        try {
            stripe.assertTestConfiguration();
            // Withdrawal identity preserves the V14 Stripe idempotency keys after migration.
            if (!a.getMetadata().containsKey("account_reference")) {
                replayingUnknownOperation = a.getMetadata().containsKey("account_started");
                a = a.checkpoint("account_started", "true");
                checkpoint.accept(a);
                String account =
                        stripe.createTestAccount(
                                request.getId(),
                                request.getDestination().substring("test-bank:".length()));
                a = a.checkpoint("account_reference", account);
                checkpoint.accept(a);
            }
            if (!a.getMetadata().containsKey("transfer_reference")) {
                replayingUnknownOperation = a.getMetadata().containsKey("transfer_started");
                a = a.checkpoint("transfer_started", "true");
                checkpoint.accept(a);
                String transfer =
                        stripe.transfer(
                                request.getId(),
                                a.getMetadata().get("account_reference"),
                                a.getAmount());
                a = a.checkpoint("transfer_reference", transfer);
                checkpoint.accept(a);
            }
            if (!a.getMetadata().containsKey("payout_reference")) {
                replayingUnknownOperation = a.getMetadata().containsKey("payout_started");
                a = a.checkpoint("payout_started", "true");
                checkpoint.accept(a);
                String payout =
                        stripe.payout(
                                request.getId(),
                                a.getMetadata().get("account_reference"),
                                a.getAmount());
                a = a.checkpoint("payout_reference", payout);
                checkpoint.accept(a);
            }
            var payout =
                    stripe.retrieve(
                            a.getMetadata().get("account_reference"),
                            a.getMetadata().get("payout_reference"));
            if (!Objects.equals(a.getMetadata().get("account_reference"), payout.accountId())
                    || !Objects.equals(a.getMetadata().get("payout_reference"), payout.id())
                    || a.getAmount() != payout.amount()
                    || !a.getCurrency().equalsIgnoreCase(payout.currency()))
                return new Result(
                        Outcome.REVIEW_REQUIRED,
                        "Provider payout identity, amount or currency mismatch");
            if ("paid".equals(payout.status())) return new Result(Outcome.PAID, null);
            if (Set.of("failed", "canceled").contains(payout.status()))
                return new Result(Outcome.FAILED, "Provider payout " + payout.status());
            return new Result(Outcome.PENDING, null);
        } catch (StripePayoutPort.ProviderException ex) {
            if (!ex.retryable() && !a.getMetadata().containsKey("payout_reference")) {
                if (replayingUnknownOperation)
                    return new Result(
                            Outcome.REVIEW_REQUIRED,
                            "Previous provider operation has an unresolved outcome");
                return new Result(Outcome.FAILED, ex.getMessage());
            }
            throw ex;
        }
    }

    @Override
    public void compensate(WithdrawalRequest request, PayoutAttempt attempt) {
        String transfer = attempt.getMetadata().get("transfer_reference");
        if (transfer != null) stripe.reverseTransfer(request.getId(), transfer);
    }

    @Override
    public Notice verifyWebhook(String body, String signature) {
        var notice = stripe.verifyWebhook(body, signature);
        return notice == null
                ? null
                : new Notice(
                        notice.eventId(),
                        notice.withdrawalId(),
                        notice.accountId(),
                        notice.payoutId(),
                        notice.amount(),
                        notice.currency());
    }
}
