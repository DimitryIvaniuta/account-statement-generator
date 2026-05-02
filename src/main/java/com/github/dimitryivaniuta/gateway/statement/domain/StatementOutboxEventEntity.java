package com.github.dimitryivaniuta.gateway.statement.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Represents an outbox event created after a new statement artifact is stored.
 */
@Table("statement_outbox_event")
public record StatementOutboxEventEntity(
        @Id UUID id,
        @Column("aggregate_type") String aggregateType,
        @Column("aggregate_id") String aggregateId,
        @Column("event_type") String eventType,
        @Column("event_key") String eventKey,
        @Column("payload_text") String payloadText,
        @Column("publish_attempts") int publishAttempts,
        @Column("next_attempt_at") Instant nextAttemptAt,
        @Column("last_error") String lastError,
        @Column("claimed_by") String claimedBy,
        @Column("claimed_at") Instant claimedAt,
        @Column("created_at") Instant createdAt,
        @Column("published_at") Instant publishedAt) {
}
