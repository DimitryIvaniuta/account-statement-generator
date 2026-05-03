package com.github.dimitryivaniuta.gateway.statement.model;

import java.time.Instant;

/**
 * Lightweight immutable revision summary used by audit endpoints.
 *
 * @param statementId deterministic statement identifier.
 * @param revision immutable revision number.
 * @param sourceChecksum source checksum.
 * @param outputChecksum output checksum.
 * @param generatedAt generation timestamp.
 * @param entryCount number of generated lines.
 * @param payloadSizeBytes exact UTF-8 payload size.
 * @param generatorVersion generator version string.
 * @param jobExecutionId originating batch execution id when available.
 */
public record StatementRevisionView(
        String statementId,
        int revision,
        String sourceChecksum,
        String outputChecksum,
        Instant generatedAt,
        int entryCount,
        long payloadSizeBytes,
        String generatorVersion,
        String jobExecutionId) {
}
