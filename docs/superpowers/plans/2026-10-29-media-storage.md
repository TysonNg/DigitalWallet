# Shared media storage implementation

Approved scope: backend/API, local source-built MinIO with S3-compatible adapter, private owner-only PNG/JPEG <=5 MiB and <=20MP. No avatar UI, QR generation, PDFs or public sharing.

## Work items
- [x] Domain metadata and status model; application storage/repository ports.
- [x] Signature/dimension/decode validation and lifecycle service with compensation/retry.
- [x] PostgreSQL V19 migration and independent short repository transactions.
- [x] AWS SDK v2 adapter, opt-in configuration and 10-minute cleanup job.
- [x] JWT owner-scoped upload/metadata/content/delete APIs and JSON errors.
- [x] Pinned-source MinIO Docker build, private bucket init, scoped application user and persistent volume.
- [x] Unit, API and opt-in real PostgreSQL/MinIO tests; operational documentation.
- [x] Independent review completed; important recovery/error-handling findings fixed with RED/GREEN regression tests. External integration verification remains dependent on Docker Engine.

## Implementation decisions
- Work on codex/media-storage; native Git access/writes require sandbox escalation. Existing checkout retained; no user changes were present at start.
- JDBC persistence uses REQUIRES_NEW to keep durable state separate from object I/O and callers' transactions.
- AWS SDK 2.31.54; MinIO and mc commits are explicit in the Dockerfile.
- No presigned URL in v1: binary content streams through authenticated backend.
- Database tombstones remain after deletion to support idempotent DELETE.
- Integration tests use an isolated random schema; actual container restart persistence is an operational check, not an automatic restart of shared services.

## Verification ledger
- Baseline Maven backend tests: passed before implementation.
- ImageValidatorTest: observed RED for missing implementation, then GREEN.
- MediaServiceTest: observed RED for missing service, then GREEN.
- MediaControllerTest: observed RED for missing controller; initial GREEN attempt exposed Mockito re-stubbing of an already throwing mock, corrected with doThrow; subsequent suite passed.
- JdbcMediaRepositoryTest: H2 PostgreSQL-mode SQL/query tests passed.
- Compose validation exposed an initialization self-dependency during editing; corrected before delivery.
## Review resolutions
- Lost READY commit acknowledgement: recover the committed READY record; require durable DELETING before compensation; preserve bytes when the database cannot be queried. Regression tests observed RED then GREEN.
- Cleanup starvation: advance failed deletion attempt timestamps to rotate the batch fairly. Regression test observed RED then GREEN.
- Transaction-start/commit exception leakage: scoped media advice maps TransactionException as well as DataAccessException to sanitized 503. Regression test observed RED before correction.
- Compose default-off feedback was reconciled with the approved scope: default false in application.yaml, intentionally true in local Compose after initialization. New credentials are documented in .env.example and the setup guide; existing private .env was not modified.
- Independent repository transactions are verified against rollback of an outer caller transaction.
- Docker Engine is not available (missing dockerDesktopLinuxEngine named pipe); GUI and CLI startup attempts did not bring it online. Source build, real PostgreSQL/MinIO integration and container-restart persistence check therefore remain unexecuted; reproducible instructions and opt-in integration test are included.
## Final verification
- Full command: mvn -q -f backend/pom.xml test; exit code 0.
- Surefire totals: 274 tests, 0 failures, 0 errors, 23 skipped external-service tests. New media tests: 27 total, 1 opt-in integration skipped.
- Docker Compose config --quiet: passed with temporary test credentials; git diff --check passed.
- No Docker image build, real PostgreSQL/MinIO execution or actual restart verification was claimed. No commit, push or merge was performed.

## Real service verification, 2026-10-29
- Docker Engine became available. Pinned-source MinIO image built successfully.
- Added isolated compose.test.yml and verify.ps1; no main application data changed.
- Initialization ran twice successfully; real PostgreSQL/MinIO integration passed including restart durability, anonymous denial, owner checks and idempotent deletion (1 passed, 0 skipped).
- Fixed Windows Docker template argument quoting in the restart harness by using JSON label output.
- All generated test containers, network and volumes were removed by the harness.
