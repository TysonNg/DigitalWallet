# Kafka withdrawal integration

Goal: Trigger existing withdrawal processing through Kafka while retaining database checkpoints, idempotency and periodic reconciliation.
Architecture: Creation and verified webhooks enqueue a transactional PostgreSQL outbox record. A publisher sends versioned messages keyed by withdrawal UUID and marks rows published only after broker acknowledgement. A record listener invokes ProcessWithdrawalUseCase. Bounded retries forward failed records to a DLT; DLT publishing failure must not acknowledge the source. Database reconciliation remains a recovery path.

- [ ] Add queue enqueue regression tests before implementation.
- [ ] Add application queue port and V16 transactional outbox adapter.
- [ ] Add Spring Kafka producer, consumer, retry/DLT configuration and tests.
- [ ] Add local KRaft Kafka Docker service and configurable application properties.
- [ ] Verify backend suite, isolated PostgreSQL migration/atomicity and embedded broker behavior.
- [ ] Document operation, recovery and limitations; review changes.

Constraints: Preserve dirty working changes and V14/V15. No live Stripe calls or migration on the application database. Messages contain identifiers only, never PINs or bank details. At-least-once delivery: DB state/provider keys remain responsible for deduplication. Local single broker settings must be documented as development only.
