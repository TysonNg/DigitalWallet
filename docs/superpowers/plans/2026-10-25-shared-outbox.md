# Shared transactional outbox

Goal: Persist generic integration events atomically with withdrawal creation; do not publish Kafka messages yet.
Architecture: Application port OutboxEventWriter accepts a generic envelope. OutboxEventMapper serializes payload to JSON; OutboxEventWriterImpl uses Spring Data JPA and requires an existing transaction with MANDATORY propagation. V17 creates outbox_event with publisher retry metadata, no business-specific foreign key. Creation appends after request and attempt persistence, before transaction commit. Idempotency replay and rejected requests must not append events.

- [x] Add application tests for event metadata, replay and failure propagation.
- [x] Implement generic envelope/writer port, V17 and JPA entity/mapper/repository/adapter.
- [x] Verify JPA atomicity using isolated PostgreSQL: commit/replay, outbox constraint failure, outer rollback and transaction requirement.
- [x] Run backend suite, review and document this step without committing.

No publisher, consumer, webhook enqueue, live provider calls or application-database migration in this step. Preserve all existing uncommitted Kafka configuration. Retire the unused withdrawal-only queue port.

Initial JDBC validation (before the approved JPA refactor): full backend suite passed, 204 tests, 0 failures/errors/skips, with real local Kafka and isolated PostgreSQL. Final review found no remaining critical/important issues. No commit or application-database migration performed.

## Approved JPA refactor

Ruling: Keep OutboxEvent/OutboxEventWriter in application; replace the JDBC adapter with OutboxEventEntity, OutboxEventMapper, SpringDataOutboxEventRepository and OutboxEventWriterImpl, matching project persistence conventions. Keep V17 unchanged. MANDATORY propagation joins the existing business transaction; saveAndFlush surfaces constraints without committing it. No publisher/consumer and no commit.

- [x] Adapt PostgreSQL tests to JPA writer and transaction proxy; add duplicate-ID protection.
- [x] Implement entity/mapper/repository/writer and remove JdbcOutboxEventWriter.
- [x] Verify PostgreSQL atomicity, run backend suite and complete review.
- [x] Update architecture documentation.
JPA refactor validation: 204 tests passed, 0 failures/errors/skips, including five real PostgreSQL outbox tests and real local Kafka connectivity. Final scoped review found no actionable issues. Temporary PostgreSQL cluster removed after verification; no commit or application-database migration.
