CREATE TABLE statement_outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    event_key VARCHAR(128) NOT NULL,
    payload_text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_statement_outbox_unpublished ON statement_outbox_event(published_at, created_at);
