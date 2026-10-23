# One held withdrawal per wallet

Baseline snapshot: 7679c0f (includes interrupted Kafka scaffolding).

Implement idempotency lookup before held-request lookup, both under existing wallet row lock. Block PENDING/PROCESSING/REVIEW_REQUIRED with HTTP 409; allow completed/failed/returned requests. Add V16 partial unique index; abort on existing duplicate holds without changing money or status. Verify application replay/block cases, API status, JPA query and PostgreSQL concurrency/index/migration preflight. Remove unimplemented Kafka dependency from runtime constructors to preserve current scheduler flow.

Verification: mvn clean test with isolated withdrawal.test.jdbc PostgreSQL URL passed: 189 tests, 0 failures/errors/skips, including 3 PostgreSQL tests. Read-only review found no critical/important issues. Application database migrations were not executed.
