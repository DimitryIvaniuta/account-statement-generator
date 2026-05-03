package com.github.dimitryivaniuta.gateway.statement.service;

import com.github.dimitryivaniuta.gateway.statement.domain.LedgerEntryEntity;
import com.github.dimitryivaniuta.gateway.statement.model.StatementLine;
import com.github.dimitryivaniuta.gateway.statement.model.StatementPayload;
import com.github.dimitryivaniuta.gateway.statement.model.StatementTotals;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds immutable statement payloads from ordered ledger entries.
 */
@Service
public class StatementPayloadFactory {

    /**
     * Creates a statement payload.
     *
     * @param statementId deterministic statement identifier.
     * @param accountId account identifier.
     * @param month statement month.
     * @param sourceChecksum source checksum.
     * @param openingBalanceMinor opening balance.
     * @param entries ordered month entries.
     * @return statement payload.
     */
    public StatementPayload create(
            final String statementId,
            final String accountId,
            final YearMonth month,
            final String sourceChecksum,
            final long openingBalanceMinor,
            final List<LedgerEntryEntity> entries) {
        long runningBalance = openingBalanceMinor;
        long totalCreditsMinor = 0L;
        long totalDebitsMinor = 0L;
        List<StatementLine> lines = new ArrayList<>(entries.size());
        String currency = entries.isEmpty() ? "N/A" : entries.get(0).currency();

        for (LedgerEntryEntity entry : entries) {
            runningBalance += entry.amountMinor();
            if (entry.amountMinor() >= 0) {
                totalCreditsMinor += entry.amountMinor();
            } else {
                totalDebitsMinor += Math.abs(entry.amountMinor());
            }
            lines.add(new StatementLine(
                    entry.id().toString(),
                    entry.bookedAt(),
                    entry.valueAt(),
                    entry.amountMinor(),
                    entry.currency(),
                    entry.reference(),
                    entry.description(),
                    entry.externalId(),
                    runningBalance));
        }

        StatementTotals totals = new StatementTotals(
                openingBalanceMinor,
                totalCreditsMinor,
                totalDebitsMinor,
                runningBalance);

        return new StatementPayload(
                "statement-v1",
                statementId,
                accountId,
                month.toString(),
                currency,
                sourceChecksum,
                totals,
                List.copyOf(lines));
    }
}
