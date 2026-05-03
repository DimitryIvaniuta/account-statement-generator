ALTER TABLE statement_outbox_event
    ADD COLUMN publish_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMPTZ,
    ADD COLUMN last_error TEXT,
    ADD COLUMN claimed_by VARCHAR(64),
    ADD COLUMN claimed_at TIMESTAMPTZ;

UPDATE statement_outbox_event
SET next_attempt_at = created_at
WHERE next_attempt_at IS NULL;

CREATE INDEX idx_statement_outbox_ready
    ON statement_outbox_event (published_at, next_attempt_at, created_at);
