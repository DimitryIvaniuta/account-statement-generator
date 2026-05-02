package com.github.dimitryivaniuta.gateway.statement.repository;

import com.github.dimitryivaniuta.gateway.statement.domain.StatementOutboxEventEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive access to statement outbox events.
 */
public interface StatementOutboxEventRepository extends ReactiveCrudRepository<StatementOutboxEventEntity, UUID> {

    /**
     * Loads the next ready unpublished outbox batch.
     *
     * @param now current time used for retry filtering.
     * @return unpublished ready outbox rows.
     */
    @Query("""
            SELECT *
            FROM statement_outbox_event
            WHERE published_at IS NULL
              AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
            ORDER BY created_at ASC
            LIMIT 100
            """)
    Flux<StatementOutboxEventEntity> findReadyBatch(@Param("now") Instant now);

    /**
     * Claims an outbox row for one relay worker.
     *
     * @param id outbox identifier.
     * @param workerId relay worker identifier.
     * @param claimedAt claim timestamp.
     * @param claimExpiredBefore ownership timeout threshold.
     * @return number of updated rows.
     */
    @Query("""
            UPDATE statement_outbox_event
            SET claimed_by = :workerId,
                claimed_at = :claimedAt
            WHERE id = :id
              AND published_at IS NULL
              AND (claimed_by IS NULL OR claimed_at < :claimExpiredBefore)
            """)
    Mono<Integer> claim(
            @Param("id") UUID id,
            @Param("workerId") String workerId,
            @Param("claimedAt") Instant claimedAt,
            @Param("claimExpiredBefore") Instant claimExpiredBefore);

    /**
     * Marks an outbox row as published.
     *
     * @param id outbox row identifier.
     * @param workerId relay worker identifier.
     * @param publishedAt publication timestamp.
     * @return number of updated rows.
     */
    @Query("""
            UPDATE statement_outbox_event
            SET published_at = :publishedAt,
                claimed_by = NULL,
                claimed_at = NULL,
                last_error = NULL
            WHERE id = :id
              AND claimed_by = :workerId
              AND published_at IS NULL
            """)
    Mono<Integer> markPublished(
            @Param("id") UUID id,
            @Param("workerId") String workerId,
            @Param("publishedAt") Instant publishedAt);

    /**
     * Stores failure details and schedules a retry.
     *
     * @param id outbox row identifier.
     * @param workerId relay worker identifier.
     * @param publishAttempts total publish attempts after the failure.
     * @param nextAttemptAt next retry timestamp.
     * @param lastError truncated failure detail.
     * @return number of updated rows.
     */
    @Query("""
            UPDATE statement_outbox_event
            SET publish_attempts = :publishAttempts,
                next_attempt_at = :nextAttemptAt,
                last_error = :lastError,
                claimed_by = NULL,
                claimed_at = NULL
            WHERE id = :id
              AND claimed_by = :workerId
              AND published_at IS NULL
            """)
    Mono<Integer> markFailed(
            @Param("id") UUID id,
            @Param("workerId") String workerId,
            @Param("publishAttempts") int publishAttempts,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("lastError") String lastError);
}
