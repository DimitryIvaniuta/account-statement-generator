package com.github.dimitryivaniuta.gateway.statement.model;

/**
 * Combines the stored statement payload and its metadata.
 *
 * @param metadata audit metadata.
 * @param payloadText canonical JSON artifact.
 */
public record GeneratedStatement(StatementMetadataView metadata, String payloadText) {
}
