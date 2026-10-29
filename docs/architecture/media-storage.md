# Private media storage

The backend stores PNG/JPEG bytes in a private S3-compatible bucket and metadata in PostgreSQL (`media_files`, migration V19). No image bytes, public object URLs or presigned URLs are stored in the database. HTTP clients cannot choose an owner, object key or bucket. Every read and delete checks the JWT subject against the file owner; administrators have no implicit bypass.

## Configuration and local startup

Media is disabled by default (`MEDIA_ENABLED=false`); its controller, S3 client and cleanup job are only registered when enabled. Flyway still creates the metadata table. Set these variables when running the backend outside Compose:

| Variable | Default / meaning |
| --- | --- |
| MEDIA_ENABLED | false |
| MEDIA_S3_ENDPOINT | Empty for AWS; http://localhost:9000 for local MinIO |
| MEDIA_S3_REGION | us-east-1 |
| MEDIA_S3_BUCKET | digital-wallet-media |
| MEDIA_S3_PATH_STYLE | false; true for local MinIO |
| MEDIA_S3_ACCESS_KEY / MEDIA_S3_SECRET_KEY | Optional pair; otherwise AWS default credentials chain |

For Docker, copy the new media entries from `environment/.env.example` into your ignored `environment/.env`. Replace the sample root and application passwords with separate random values. Never use root credentials for the backend. Run from the repository root:

```powershell
docker compose -f environment/docker-compose.yml config --quiet
docker compose -f environment/docker-compose.yml up -d --build minio minio-init
# Backend depends on successful initialization and enables media automatically.
docker compose -f environment/docker-compose.yml up -d --build backend
```

The image builds MinIO and mc from the commits pinned in `environment/media/Dockerfile`. First build requires GitHub/Go proxy network access and can take several minutes. Review and update source pins periodically. Data persists in the Compose `media_data` volume. S3 API is bound to loopback port 9000; console port 9001. Initialization is repeatable: it creates the bucket, disables anonymous access, creates a bucket-scoped policy and application credentials. Do not reuse the application's bucket for unrelated files.

## API

Base path: `/api/v1/media`. All endpoints require `Authorization: Bearer <JWT>`. JSON uses the existing ApiResponse envelope. Binary downloads do not. `contentUrl` is an authenticated backend-relative path, not a storage URL. Browser `<img>` cannot attach a bearer header; a future frontend integration should use its authenticated server-side proxy or fetch a Blob.

```powershell
# Set $token to your authenticated access token; do not commit it.
curl.exe -H "Authorization: Bearer $token" -F "file=@C:/images/photo.png" http://localhost:3006/api/v1/media
# Use data.id from the upload response:
curl.exe -H "Authorization: Bearer $token" http://localhost:3006/api/v1/media/<id>
curl.exe -H "Authorization: Bearer $token" http://localhost:3006/api/v1/media/<id>/content --output downloaded.png
curl.exe -X DELETE -H "Authorization: Bearer $token" http://localhost:3006/api/v1/media/<id>
```

Upload returns 201 with `{id, originalName, contentType, sizeBytes, createdAt, contentUrl}` under `data`. File names and declared MIME types are untrusted: validation detects signatures, verifies the image decoder format and dimensions before decoding, then confirms decoding succeeds. Limit: 5 MiB and 20 million pixels. PNG/JPEG only; no PDF, SVG, GIF or WebP. Original bytes are retained. Filenames are display metadata and never storage paths.

Reads return 404 for missing/non-owned/non-READY files. Invalid images return 415; oversized uploads 413; unavailable storage 503. Download responses include verified Content-Type, Content-Length, `Cache-Control: private, no-store`, and `X-Content-Type-Options: nosniff`.

Deletion returns 204 after the durable deletion request is recorded; physical deletion may finish asynchronously. Repeated deletion by the owner is safe. Tombstones remain in metadata for idempotency. There is no list API in v1; callers retain the uploaded file ID for future business references.

## Lifecycle and recovery

- Upload: commit PENDING metadata, put object, compare-and-set READY. No database transaction spans storage I/O. Storage calls have bounded timeouts.
- Failed upload: resolve an ambiguous READY commit by rereading metadata. A committed READY file is returned successfully; compensation only deletes bytes after a durable DELETING state. If the database is unavailable, preserve bytes so a READY record never points to a deleted image. PENDING records are retried by cleanup.
- Delete: commit DELETING before attempting physical deletion, then mark DELETED. Reads are blocked as soon as deletion is recorded.
- Cleanup every 10 minutes: up to 100 oldest candidates, PENDING older than 1 hour and DELETING. Conditional state updates avoid promoting stale uploads; deleting an absent object is successful. Failed deletion attempts advance their timestamp so later candidates are not starved.
- Trusted backend features can call `storeGeneratedImage(owner, name, bytes)`; clients cannot request this mode. No QR generation or avatar assignment is included.

Metrics: `media.upload.errors`, `media.delete.errors`, `media.cleanup.errors` (Prometheus names normalized with underscores). Inspect operation logs by mediaId; logs include operation/result/duration, never credentials or image data. Existing JVM/Mockito warnings are unrelated to media.

## Verification

```powershell
mvn -f backend/pom.xml test
```

Unit/API tests run without external services. The real PostgreSQL/MinIO integration test is opt-in and skips unless both properties below are provided. Use an isolated test database whose user may create/drop schemas. Credentials are environment variables:

```powershell
$env:MEDIA_TEST_DB_USER='testuser'
$env:MEDIA_TEST_DB_PASSWORD='<test database password>'
$env:MEDIA_S3_ACCESS_KEY='<initialized MinIO app key>'
$env:MEDIA_S3_SECRET_KEY='<initialized MinIO app secret>'
mvn -f backend/pom.xml -pl digitalwallet-infrastructure -am test '-Dtest=MediaStorageIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dmedia.test.jdbc=jdbc:postgresql://localhost:5432/media_test' '-Dmedia.test.endpoint=http://localhost:9000'
```

The integration test creates a random database schema, applies V19, checks byte-for-byte downloads and anonymous denial, then deletes its objects/schema. It never restarts shared containers. The isolated harness below labels its own MinIO container and verifies the label before restarting it. To verify volume persistence, upload a file using the API, restart only MinIO with `docker compose -f environment/docker-compose.yml restart minio`, and download the same ID again; bytes must match. Retain the metadata database throughout this check.

## Backup and cloud migration

Back up both the PostgreSQL database and bucket contents, preserving exact object keys. Pause media writes/deletes/cleanup during a coordinated backup or migration so the two datasets represent the same point in time. Restore both before enabling media. Never run `docker compose down -v` when retaining local files matters.

For cloud, provision a private bucket and credentials with Get/Put/DeleteObject permissions scoped to that bucket. Copy existing objects with unchanged keys and migrate `media_files.bucket` values if the destination bucket name changes. Change endpoint, region, credentials and path-style settings as required, verify access/round trips, then enable writes. Changing endpoint alone does not move existing files. CDN, public sharing, KYC, documents, antivirus workflows and direct-to-storage browser uploads are outside this version.
### Automated isolated verification

From the repository root, build once, then run the PowerShell harness:

```powershell
docker build -t digital-wallet-minio:local environment/media
./environment/media/verify.ps1
```

The harness creates a uniquely named test project, generates temporary credentials, starts PostgreSQL on 15432 and MinIO on 19000, initializes the private bucket twice, runs the real integration test and restarts only its labeled MinIO container. It checks exact image bytes after restart and removes only that invocation's test containers and volumes. Main application containers and data are untouched. Those two host ports must be free.

Verified on 2026-10-29: source image build, repeated init, PostgreSQL V19 migration, upload/read, rejection of another owner and anonymous reads, real MinIO restart with identical bytes, idempotent deletion. Integration result: 1 test passed, 0 skipped. A clean Maven build is used because IDE-generated stale class files were observed in target/test-classes.