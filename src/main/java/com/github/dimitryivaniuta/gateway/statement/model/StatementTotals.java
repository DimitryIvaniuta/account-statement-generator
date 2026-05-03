package com.github.dimitryivaniuta.gateway.statement.model;

/**
 * Summarizes statement balances and movement totals in minor units.
 *
 * @param openingBalanceMinor balance before the month.
 * @param totalCreditsMinor sum of positive amounts.
 * @param totalDebitsMinor sum of absolute values of negative amounts.
 * @param closingBalanceMinor balance after the last entry.
 */
public record StatementTotals(
        long openingBalanceMinor,
        long totalCreditsMinor,
        long totalDebitsMinor,
        long closingBalanceMinor) {
}
