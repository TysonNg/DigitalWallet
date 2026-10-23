package com.tyson.digitalwallet.ddd.application.port.payment;

import java.util.UUID;

/** All implementations must use Stripe sandbox/test credentials only. */
public interface StripePayoutPort {
    void assertTestConfiguration();

    String createTestAccount(UUID withdrawalId, String testBank);

    String transfer(UUID withdrawalId, String accountId, long usdCents);

    String payout(UUID withdrawalId, String accountId, long usdCents);

    PayoutState retrieve(String accountId, String payoutId);

    void reverseTransfer(UUID withdrawalId, String transferId);

    WebhookNotice verifyWebhook(String rawBody, String signature);

    record PayoutState(String id, String accountId, long amount, String currency, String status) {}

    record WebhookNotice(
            UUID withdrawalId,
            String accountId,
            String payoutId,
            long amount,
            String currency,
            String eventId) {}

    class ProviderException extends RuntimeException {
        private final boolean retryable;

        public ProviderException(String message, boolean retryable, Throwable cause) {
            super(message, cause);
            this.retryable = retryable;
        }

        public boolean retryable() {
            return retryable;
        }
    }
}
