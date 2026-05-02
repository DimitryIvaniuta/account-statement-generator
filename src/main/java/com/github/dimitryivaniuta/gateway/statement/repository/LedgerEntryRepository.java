package com.github.dimitryivaniuta.gateway.statement.repository;

import com.github.dimitryivaniuta.gateway.statement.domain.LedgerEntryEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive access to ledger entries.
 */
public interface LedgerEntryRepository extends ReactiveCrudRepository<LedgerEntryEntity, UUID> {

    /**
     * Loads all entries for one account and month in deterministic order.
     *
     * @param accountId account identifier.
     * @param from inclusive month start.
     * @param to exclusive month end.
     * @return ordered ledger entries.
     */
    @Query("""
            SELECT *
            FROM ledger_entry
            WHERE account_id = :accountId
              AND booked_at >= :from
              AND booked_at < :to
            ORDER BY booked_at ASC, id ASC
            """)
    Flux<LedgerEntryEntity> findForAccountAndMonth(
            @Param("accountId") String accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Lists accounts that have any activity in the target month.
     *
     * @param from inclusive month start.
     * @param to exclusive month end.
     * @return distinct account identifiers.
     */
    @Query("""
            SELECT DISTINCT account_id
            FROM ledger_entry
            WHERE booked_at >= :from
              AND booked_at < :to
            ORDER BY account_id ASC
            """)
    Flux<String> findDistinctAccountIdsForMonth(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Calculates the opening balance before the month.
     *
     * @param accountId account identifier.
     * @param before exclusive upper bound.
     * @return opening balance in minor units.
     */
    @Query("""
            SELECT COALESCE(SUM(amount_minor), 0)
            FROM ledger_entry
            WHERE account_id = :accountId
              AND booked_at < :before
            """)
    Mono<Long> sumBalanceBefore(
            @Param("accountId") String accountId,
            @Param("before") LocalDateTime before);
}
