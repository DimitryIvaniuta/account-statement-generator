package com.github.dimitryivaniuta.gateway.statement.model;

import java.time.Instant;

/**
 * Event emitted for a newly stored statement artifact.
 *
 * @param statementId statement identifier.
 * @param accountId account identifier.
 * @param statementMonth month in yyyy-MM format.
 * @param revision immutable revision number.
 * @param sourceChecksum source checksum.
 * @param outputChecksum output checksum.
 * @param contentType stored media type.
 * @param payloadSizeBytes exact UTF-8 payload size.
 * @param generatedAt generation timestamp.
 */
public record StatementGeneratedEvent(
        String statementId,
        String accountId,
        String statementMonth,
        int revision,
        String sourceChecksum,
        String outputChecksum,
        String contentType,
        long payloadSizeBytes,
        Instant generatedAt) {
}
