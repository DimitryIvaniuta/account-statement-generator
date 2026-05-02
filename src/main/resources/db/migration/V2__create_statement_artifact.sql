CREATE TABLE statement_artifact (
    id UUID PRIMARY KEY,
    statement_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    statement_month DATE NOT NULL,
    revision INTEGER NOT NULL,
    source_checksum CHAR(64) NOT NULL,
    output_checksum CHAR(64) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    payload_text TEXT NOT NULL,
    entry_count INTEGER NOT NULL,
    generator_version VARCHAR(32) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    job_execution_id VARCHAR(64),
    CONSTRAINT uk_statement_artifact_statement_id UNIQUE (statement_id, revision),
    CONSTRAINT uk_statement_artifact_source UNIQUE (account_id, statement_month, source_checksum),
    CONSTRAINT uk_statement_artifact_revision UNIQUE (account_id, statement_month, revision)
);

CREATE INDEX idx_statement_artifact_lookup ON statement_artifact(account_id, statement_month, revision DESC);
