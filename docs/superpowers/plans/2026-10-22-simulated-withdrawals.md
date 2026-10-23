# Simulated Withdrawals Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement task by task.

**Goal:** Complete Stripe Connect sandbox withdrawals with real API requests and signed webhooks, without real money movement.

**Architecture:** Persist requests and reserve wallet funds transactionally. A worker creates test Connect accounts, USD transfers and payouts using stable Stripe idempotency keys. Signed Connect webhooks and reconciliation retrieve Stripe state before settling or reversing the hold. UI uses only published US test bank details, fixed demo conversion 25,000 VND/USD, and owner-authorized history.

**Tech Stack:** Java, Spring, JPA, Flyway, Next.js, TypeScript.

**Spec:** User-approved Stripe-only API/webhook sandbox flow. Internal success/failure simulator is superseded and removed. No live mode.

## Global Constraints
- Only sk_test_/rk_test_ Stripe keys; reject livemode=true API results and webhooks. No payOS payouts.
- Simulation must be visible in UI and API responses.
- Preserve PIN and wallet ownership checks.
- Retry cannot reserve, settle or refund twice.

## Review Focus
- Duplicate concurrent requests: one hold only.
- Reusing keys with changed destinations/amounts: reject.
- Failed withdrawals: restore available balance once without a deposit transaction.
- Restart: persisted pending requests remain processable.
- Forged wallet identifiers: owner authorization on writes and reads.

### Task 1: Persistent withdrawal state and hold
- [ ] Add behavioral tests for reserve, success, failure, duplicate requests and invalid inputs.
- [ ] Run tests and confirm failure before implementing.
- [ ] Add domain withdrawal request/repository, migration and JPA adapter.
- [ ] Implement transactional creation, Stripe payout port, resumable worker and owner history.
- [ ] Verify backend tests.

### Task 2: Simulation UI and API
- [ ] Add validated request API, history endpoint, signed webhook and reconciliation worker.
- [ ] Replace hardcoded bank details with input and explicit simulation outcomes.
- [ ] Keep stable idempotency keys across transport retries and refresh status/history.
- [ ] Verify frontend tests, typecheck and lint.

### Task 3: Documentation and review
- [ ] Document simulator scenarios and no external-money behavior.
- [ ] Review changed code and report verification and limitations.
