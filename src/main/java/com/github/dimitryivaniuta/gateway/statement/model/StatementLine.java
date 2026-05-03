package com.github.dimitryivaniuta.gateway.statement.model;

import java.time.LocalDateTime;

/**
 * Represents a single statement line in deterministic output order.
 *
 * @param entryId ledger entry identifier.
 * @param bookedAt booking timestamp.
 * @param valueAt value timestamp.
 * @param amountMinor signed amount in minor units.
 * @param currency ISO currency code.
 * @param reference business reference.
 * @param description business description.
 * @param externalId external source identifier.
 * @param runningBalanceMinor running balance after this entry.
 */
public record StatementLine(
        String entryId,
        LocalDateTime bookedAt,
        LocalDateTime valueAt,
        long amountMinor,
        String currency,
        String reference,
        String description,
        String externalId,
        long runningBalanceMinor) {
}
