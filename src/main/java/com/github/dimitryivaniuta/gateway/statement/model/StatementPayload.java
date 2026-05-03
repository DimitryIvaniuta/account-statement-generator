package com.github.dimitryivaniuta.gateway.statement.model;

import java.util.List;

/**
 * Immutable business payload stored as the statement artifact.
 *
 * @param schemaVersion payload schema version.
 * @param statementId deterministic statement identifier.
 * @param accountId account identifier.
 * @param statementMonth month in yyyy-MM format.
 * @param currency statement currency.
 * @param sourceChecksum checksum of the canonical source snapshot.
 * @param totals computed balances and totals.
 * @param entries statement lines.
 */
public record StatementPayload(
        String schemaVersion,
        String statementId,
        String accountId,
        String statementMonth,
        String currency,
        String sourceChecksum,
        StatementTotals totals,
        List<StatementLine> entries) {
}
