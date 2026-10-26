# Withdrawals

Withdrawal requests are business records. Provider executions are stored separately in payout_attempts; adding a provider does not require another withdrawal table.

## Application boundaries

- WithdrawalController maps HTTP DTOs to CreateWithdrawalCommand and calls use-case interfaces.
- Each use-case contract has its own implementation in the impl package (creation, history, execution, notification and reconciliation). Database transactions are short; provider calls run outside them under a renewable distributed lock.
- PayoutGatewayRegistry selects the provider gateway persisted on the attempt. StripePayoutGateway owns Connect account/transfer/payout orchestration and demo conversion. StripePayoutPort is an infrastructure-facing Stripe integration contract, not the common business gateway.
- Requests use WithdrawalStatus; executions use PayoutAttemptStatus. Standard entity getters and dedicated JPA mappers follow the existing layer convention.

## Money and recovery

Wallet.balance is available balance. A held withdrawal is identified by the withdrawal UUID and its PENDING, PROCESSING or REVIEW_REQUIRED state. The ledger liability remains unchanged until settlement. Reconciliation compares ledger balance with available balance plus held request amounts. Creation locks the wallet and commits the request, attempt and balance reservation together. Settlement/refund locks the request and wallet; both ledger entries and state changes commit atomically.

Transport retries reuse the same attempt and operation identities. Stripe operation-start markers are committed before network calls, and returned references are checkpointed after each step. An ambiguous operation followed by a rejection requires review; it cannot automatically refund. Provider compensation must finish before a local refund. The definitive-failure/compensation-pending checkpoint is committed first; recovery resumes compensation without resubmitting the payout. An already fully reversed Stripe transfer is accepted during recovery, even if its original idempotency record has expired. A payout returned after success produces a compensating journal and RETURNED state.

The schema permits numbered attempts and prevents multiple active attempts. The application currently creates one logical attempt per request and never automatically fails over after an unknown outcome. A future explicit retry/routing feature must allocate distinct provider operation identities and establish that a previous attempt cannot pay before starting another.

## Webhooks and scheduling

Signed provider notices are validated against persisted amount, currency and account/payout references, then stored in payout_notification_inbox. Its provider/event key deduplicates deliveries. Workers read the provider's current state rather than trusting webhook order. Events are marked processed only after the processor returns; failed work remains pending. Pending withdrawals are reconciled every 15 seconds; successful withdrawals are eligible for another check after one hour to detect late returns. Processing is serialized per withdrawal across instances.

## Compatibility and migration

The existing /wallet/withdrawals and /payments/webhook/stripe-connect URLs remain. Existing testBank requests default to STRIPE and map to a test-bank destination. New requests may specify provider/destination. Stripe sandbox-only guards, published test banks and fixed demo conversion remain; no real payout support is introduced.

V14 is unchanged. V15 renames its table, copies provider execution state into payout_attempts, preserves withdrawal UUIDs and Stripe operation references, migrates past returned payouts, then removes Stripe-specific columns from withdrawal_requests. Legacy missing references are conservatively marked as potentially started. Stop the old application/worker before deploying V15; old binaries cannot operate against the new schema. Back up and validate against a staging copy before production deployment.

## Verification

Run backend tests with mvn -f backend/pom.xml clean test. The PostgreSQL migration regression fixture is backend/digitalwallet-infrastructure/src/test/resources/db/withdrawal_migration_test.sql; execute it with psql against an isolated test database. It wraps its schema and assertions in a transaction and rolls back. Unit tests cover lifecycle transitions, duplicate holds, provider mismatch, authorization/PIN, pending/paid/failure, ambiguous outcomes, compensation failures, late returns and webhook validation. Persistence tests verify checkpoints after a new session and hold-aware queries. Stripe tests use a local HTTP server, not real API calls.

## One held withdrawal per wallet

Creation locks the wallet, checks idempotency first, then rejects a new key if any request is PENDING, PROCESSING or REVIEW_REQUIRED. Replays of the same key return the original request. The API returns HTTP 409 / WITHDRAWAL_ALREADY_IN_PROGRESS for a new request while funds are held. SUCCEEDED, FAILED and RETURNED requests do not block a new request.

V16 adds a PostgreSQL partial unique index on wallet_id for these held states. It aborts without changing financial data if legacy wallets already have multiple holds; reconcile those payouts before deployment. Do not clear holds merely to make migration succeed. The incomplete Kafka queue dependency was removed from use-case constructors; initial processing is selected by withdrawal.processing-mode; Kafka mode keeps scheduler recovery active. See [Kafka consumer flow](withdrawal-kafka.md).
