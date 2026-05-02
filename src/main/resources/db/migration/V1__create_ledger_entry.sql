CREATE TABLE ledger_entry (
    id UUID PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    booked_at TIMESTAMP NOT NULL,
    value_at TIMESTAMP NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    reference VARCHAR(128),
    description VARCHAR(256),
    external_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ledger_entry_account_booked_at ON ledger_entry(account_id, booked_at, id);
