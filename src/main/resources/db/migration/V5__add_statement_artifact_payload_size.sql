ALTER TABLE statement_artifact
    ADD COLUMN payload_size_bytes BIGINT;

UPDATE statement_artifact
SET payload_size_bytes = OCTET_LENGTH(CONVERT_TO(payload_text, 'UTF8'));

ALTER TABLE statement_artifact
    ALTER COLUMN payload_size_bytes SET NOT NULL;
