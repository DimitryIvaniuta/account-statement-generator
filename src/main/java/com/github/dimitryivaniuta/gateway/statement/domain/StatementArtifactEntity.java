package com.github.dimitryivaniuta.gateway.statement.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Stores a generated statement artifact and audit metadata.
 */
@Table("statement_artifact")
public record StatementArtifactEntity(
        @Id UUID id,
        @Column("statement_id") String statementId,
        @Column("account_id") String accountId,
        @Column("statement_month") LocalDate statementMonth,
        @Column("revision") int revision,
        @Column("source_checksum") String sourceChecksum,
        @Column("output_checksum") String outputChecksum,
        @Column("content_type") String contentType,
        @Column("payload_text") String payloadText,
        @Column("payload_size_bytes") long payloadSizeBytes,
        @Column("entry_count") int entryCount,
        @Column("generator_version") String generatorVersion,
        @Column("generated_at") Instant generatedAt,
        @Column("job_execution_id") String jobExecutionId) {
}
