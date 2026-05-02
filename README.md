# account-statement-generator

Auditable monthly account statement generator built with **Java 21**, **Spring Boot 4.0.5**, **WebFlux**, **Spring Batch**, **R2DBC/PostgreSQL**, **Flyway**, **Redis**, and **Kafka (KRaft)**.

## GitHub repository

- **Repository name:** `account-statement-generator`
- **Description:** `Auditable monthly account statement generator with deterministic JSON artifacts, immutable revision history, checksum-backed downloads, transactional outbox publication, and production-grade audit metadata.`

## What was upgraded in this version

- Added **reactive transaction boundaries** so statement artifact persistence and outbox event persistence succeed or fail together.
- Added **ETag / Last-Modified / 304 Not Modified** support for efficient immutable downloads.
- Added **audit revision listing** endpoint: `GET /api/v1/statements/{month}/revisions?accountId=...`.
- Added **payload size tracking** for audit and download integrity.
- Hardened the **Redis generation lock** behavior so generation no longer proceeds when the lock was not acquired.
- Upgraded the outbox relay with **claiming, retry metadata, exponential backoff, and last-error capture**.
- Fixed broken tests and added stronger unit coverage for idempotency, download caching, and revision audit paths.
- Added the required Flyway PostgreSQL database module for modern Flyway PostgreSQL support.

## Goal

Generate immutable monthly account statements from ledger entries in an audit-friendly way, where the same input snapshot always yields the same artifact bytes.

## Main characteristics

- Deterministic statement payload generation.
- Immutable stored outputs.
- Idempotent reruns for the same ledger snapshot.
- Separate **source checksum** and **output checksum**.
- Download endpoint for stored monthly statements.
- Revision history endpoint for audit review.
- Spring Batch job for monthly generation.
- Redis best-effort distributed lock to reduce concurrent duplicate work.
- PostgreSQL unique constraints as the authoritative idempotency guard.
- Kafka outbox publication after statement creation.
- Retry-aware outbox relay with claim ownership and failure diagnostics.

## High-level flow

1. Ledger entries are stored in PostgreSQL.
2. A monthly generation request launches a Spring Batch job.
3. For every account with entries in the target month:
   - opening balance is calculated,
   - monthly entries are loaded in deterministic order,
   - a source checksum is computed from the canonical source snapshot,
   - if an identical artifact already exists, it is returned,
   - otherwise a new immutable revision is stored,
   - an outbox event is written in the same reactive transaction.
4. A relay claims unpublished outbox rows and publishes them to Kafka.
5. The statement is available from `GET /api/v1/statements/{month}?accountId=...`.

## Why JSON artifact output

The requirement allows **PDF data or JSON output**. This implementation chooses **canonical JSON** because it is easier to make deterministic, checksum-friendly, diffable, and audit-friendly. The stored artifact is immutable UTF-8 text bytes with a SHA-256 digest.

## Determinism rules

To ensure **same input -> same statement**:

- ledger entries are ordered by `booked_at`, then `id`,
- the statement JSON uses stable field ordering,
- the payload excludes non-deterministic timestamps,
- generation timestamp is stored as metadata outside the payload,
- statement identity is derived from month + account + source checksum.

## Idempotency model

- **Business idempotency:** unique constraint on `(account_id, statement_month, source_checksum)`.
- **Concurrency reduction:** Redis lock per `accountId + month`.
- **Batch reruns:** repeated launches for the same month safely reuse the already stored artifact when the source snapshot is unchanged.
- **Immutable history:** changed input produces a new revision while preserving old revisions for audit.

## API

### Download latest revision

```http
GET /api/v1/statements/2026-03?accountId=ACC-1001
```

Returns the immutable JSON artifact as attachment.

### Download exact revision

```http
GET /api/v1/statements/2026-03?accountId=ACC-1001&revision=1
```

### Fetch metadata only

```http
GET /api/v1/statements/2026-03/metadata?accountId=ACC-1001
```

### Fetch audit revision history

```http
GET /api/v1/statements/2026-03/revisions?accountId=ACC-1001
```

### Trigger generation for all accounts in a month

```http
POST /api/v1/statements/generation/2026-03
```

### Trigger generation for one account only

```http
POST /api/v1/statements/generation/2026-03?accountId=ACC-1001
```

## Local run

### Infrastructure

```bash
docker compose up -d
```

### Application

```bash
./gradlew clean test bootRun
```

or on Windows:

```bat
gradlew.bat clean test bootRun
```

If Gradle is not already installed on the host, install Gradle 8.14+ first.

## Postman

See:

- `postman/account-statement-generator.postman_collection.json`
- `postman/local.postman_environment.json`

## Notes about verification in this environment

I fully updated the source tree, migrations, docs, tests, Docker Compose, and Postman assets. I could not execute the real Gradle dependency resolution and runtime verification in this container because external Maven downloads are blocked here, so the final compile/run check still needs a normal network-enabled Gradle environment.
