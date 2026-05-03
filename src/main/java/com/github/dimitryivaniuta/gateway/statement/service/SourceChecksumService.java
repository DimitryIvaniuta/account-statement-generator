package com.github.dimitryivaniuta.gateway.statement.service;

import com.github.dimitryivaniuta.gateway.statement.domain.LedgerEntryEntity;
import com.github.dimitryivaniuta.gateway.statement.support.Sha256Support;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Computes a deterministic checksum for the exact source snapshot used to create a statement.
 */
@Service
public class SourceChecksumService {

    /**
     * Computes the source checksum.
     *
     * @param accountId account identifier.
     * @param month target month.
     * @param openingBalanceMinor opening balance before the month.
     * @param entries ordered month entries.
     * @return source checksum.
     */
    public String compute(
            final String accountId,
            final YearMonth month,
            final long openingBalanceMinor,
            final List<LedgerEntryEntity> entries) {
        StringBuilder builder = new StringBuilder();
        builder.append("schemaVersion=source-checksum-v1").append('\n');
        builder.append("account=").append(accountId).append('\n');
        builder.append("month=").append(month).append('\n');
        builder.append("openingBalanceMinor=").append(openingBalanceMinor).append('\n');
        builder.append("entryCount=").append(entries.size()).append('\n');
        for (LedgerEntryEntity entry : entries) {
            builder.append(entry.id()).append('|')
                    .append(entry.bookedAt()).append('|')
                    .append(entry.valueAt()).append('|')
                    .append(entry.amountMinor()).append('|')
                    .append(entry.currency()).append('|')
                    .append(nullSafe(entry.reference())).append('|')
                    .append(nullSafe(entry.description())).append('|')
                    .append(nullSafe(entry.externalId()))
                    .append('\n');
        }
        return Sha256Support.hex(builder.toString());
    }

    private String nullSafe(final String value) {
        return value == null ? "" : value;
    }
}
