# Architecture

## Components

- **WebFlux API** exposes statement download, metadata, revision-history, and manual job trigger endpoints.
- **Spring Batch** orchestrates monthly generation by month and optional account filter.
- **R2DBC/PostgreSQL** stores ledger entries, statement artifacts, and outbox rows.
- **Flyway** manages immutable schema migrations for business tables.
- **Redis** reduces concurrent duplicate generation by applying a short-lived per-account/month lock.
- **Reactive transaction operator** keeps statement artifact persistence and outbox persistence atomic.
- **Kafka outbox relay** claims unpublished rows, retries failures with backoff, and publishes `statement.generated.v1` only after the artifact is durably stored.

## Data model

### ledger_entry
Source-of-truth ledger rows used to build statements.

### statement_artifact
Immutable generated statement revisions.

Important columns:
- `source_checksum` - checksum of the exact source snapshot.
- `output_checksum` - checksum of the exact artifact bytes.
- `payload_size_bytes` - exact UTF-8 payload size for audit/download purposes.
- `revision` - immutable version number within account + month.

### statement_outbox_event
Transactional event log for asynchronous Kafka publication.

Important columns:
- `publish_attempts` - number of relay attempts.
- `next_attempt_at` - retry backoff timestamp.
- `last_error` - last publication error detail.
- `claimed_by`, `claimed_at` - lightweight relay ownership fields.

## Determinism rules

- ordered source rows,
- stable JSON serialization,
- no generation timestamp inside payload,
- deterministic statement id,
- checksum computed from canonical source snapshot,
- artifact checksum computed from exact UTF-8 bytes returned to clients.

## Idempotency rules

- unique `(account_id, statement_month, source_checksum)` for same-input reruns,
- unique `(account_id, statement_month, revision)` for immutable versioning,
- Redis lock only as concurrency optimization,
- database uniqueness remains the authoritative duplicate guard.

## HTTP behavior

- Statement downloads expose **ETag** and **Last-Modified**.
- `If-None-Match` requests can return **304 Not Modified**.
- Specific revision downloads are immutable and audit-safe.

## Batch behavior

- The batch step is a tasklet that delegates to reactive generation logic.
- Blocking launch paths stay off the main reactive threads.
- Repeatable launches are safe because generation is idempotent at the persistence layer.
