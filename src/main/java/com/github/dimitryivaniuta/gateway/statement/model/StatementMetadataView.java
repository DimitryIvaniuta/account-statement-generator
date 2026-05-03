package com.github.dimitryivaniuta.gateway.statement.model;

import java.time.Instant;

/**
 * Audit metadata returned by the metadata endpoint.
 *
 * @param statementId deterministic statement identifier.
 * @param accountId account identifier.
 * @param statementMonth month in yyyy-MM format.
 * @param revision immutable revision number.
 * @param sourceChecksum source checksum.
 * @param outputChecksum output checksum.
 * @param generatedAt generation timestamp.
 * @param entryCount number of generated lines.
 * @param contentType artifact media type.
 * @param payloadSizeBytes exact UTF-8 payload size.
 * @param generatorVersion generator version string.
 * @param jobExecutionId originating batch execution id when available.
 */
public record StatementMetadataView(
        String statementId,
        String accountId,
        String statementMonth,
        int revision,
        String sourceChecksum,
        String outputChecksum,
        Instant generatedAt,
        int entryCount,
        String contentType,
        long payloadSizeBytes,
        String generatorVersion,
        String jobExecutionId) {
}
