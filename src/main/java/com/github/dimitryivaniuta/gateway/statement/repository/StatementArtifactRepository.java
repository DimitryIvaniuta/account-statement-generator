package com.github.dimitryivaniuta.gateway.statement.repository;

import com.github.dimitryivaniuta.gateway.statement.domain.StatementArtifactEntity;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive access to immutable statement artifacts.
 */
public interface StatementArtifactRepository extends ReactiveCrudRepository<StatementArtifactEntity, UUID> {

    /**
     * Finds the latest revision for an account and month.
     *
     * @param accountId account identifier.
     * @param statementMonth month represented as first day of month.
     * @return latest statement revision if present.
     */
    @Query("""
            SELECT *
            FROM statement_artifact
            WHERE account_id = :accountId
              AND statement_month = :statementMonth
            ORDER BY revision DESC
            LIMIT 1
            """)
    Mono<StatementArtifactEntity> findLatest(
            @Param("accountId") String accountId,
            @Param("statementMonth") LocalDate statementMonth);

    /**
     * Finds the artifact for the exact source checksum.
     *
     * @param accountId account identifier.
     * @param statementMonth month represented as first day of month.
     * @param sourceChecksum source checksum.
     * @return matching statement artifact.
     */
    @Query("""
            SELECT *
            FROM statement_artifact
            WHERE account_id = :accountId
              AND statement_month = :statementMonth
              AND source_checksum = :sourceChecksum
            ORDER BY revision DESC
            LIMIT 1
            """)
    Mono<StatementArtifactEntity> findBySourceChecksum(
            @Param("accountId") String accountId,
            @Param("statementMonth") LocalDate statementMonth,
            @Param("sourceChecksum") String sourceChecksum);

    /**
     * Finds a specific stored revision.
     *
     * @param accountId account identifier.
     * @param statementMonth month represented as first day of month.
     * @param revision revision number.
     * @return specific statement artifact.
     */
    @Query("""
            SELECT *
            FROM statement_artifact
            WHERE account_id = :accountId
              AND statement_month = :statementMonth
              AND revision = :revision
            LIMIT 1
            """)
    Mono<StatementArtifactEntity> findByRevision(
            @Param("accountId") String accountId,
            @Param("statementMonth") LocalDate statementMonth,
            @Param("revision") int revision);

    /**
     * Lists all revisions for an account and month from newest to oldest.
     *
     * @param accountId account identifier.
     * @param statementMonth month represented as first day of month.
     * @return immutable revision history.
     */
    @Query("""
            SELECT *
            FROM statement_artifact
            WHERE account_id = :accountId
              AND statement_month = :statementMonth
            ORDER BY revision DESC
            """)
    Flux<StatementArtifactEntity> findAllRevisions(
            @Param("accountId") String accountId,
            @Param("statementMonth") LocalDate statementMonth);

    /**
     * Finds the current maximum revision.
     *
     * @param accountId account identifier.
     * @param statementMonth month represented as first day of month.
     * @return highest revision or zero.
     */
    @Query("""
            SELECT COALESCE(MAX(revision), 0)
            FROM statement_artifact
            WHERE account_id = :accountId
              AND statement_month = :statementMonth
            """)
    Mono<Integer> findMaxRevision(
            @Param("accountId") String accountId,
            @Param("statementMonth") LocalDate statementMonth);
}
