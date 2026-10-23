# Provider-independent withdrawals implementation plan

Goal: Separate withdrawal business state from provider payout attempts while preserving the Stripe sandbox flow and existing API compatibility.
Architecture: Shared withdrawal requests and payout attempts, application use-case interfaces, provider gateway adapters, persistent reconciliation and webhook inbox. Preserve existing wallet ownership, PIN, idempotency and compensation behavior.

- [x] Add domain transition and attempt behavior tests; verify failures.
- [x] Implement provider-independent domain models and gateway contract.
- [x] Add migration after V14 that preserves existing data and persistence mappings.
- [x] Refactor orchestration into use cases and provider adapters with existing API compatibility.
- [x] Verify duplicate holds, unknown outcomes, restart recovery, webhook duplication and returned payouts.
- [x] Run backend tests and review the complete diff.

Constraints: Do not overwrite unrelated working changes. Do not execute database migrations against a live database. Keep sandbox-only Stripe protection. Retry must never duplicate a debit or payout. Unknown outcomes must keep funds held. Preserve Flyway V14 checksum.

Verification: backend clean test passed; PostgreSQL 18 isolated V14?V15 migration/inbox fixture passed; frontend TypeScript passed. Frontend suite: 71/73 passed, two unrelated ai-draft reset tests fail because clearAiConversationAction is missing from the test mock. Targeted withdrawal frontend tests and lint checked separately.

Final backend verification: 179 tests, 0 failures, 0 errors, 0 skipped. Frontend withdrawal tests: 10/10. TypeScript and targeted ESLint passed. Temporary PostgreSQL stopped after migration verification.

Final review: operation ambiguity, already-reversed recovery and durable compensation phase findings addressed with regression tests. No remaining critical/important findings in reviewed fixes.
