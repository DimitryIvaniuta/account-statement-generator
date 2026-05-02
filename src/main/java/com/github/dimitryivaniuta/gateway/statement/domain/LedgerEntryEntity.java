package com.github.dimitryivaniuta.gateway.statement.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Represents an immutable ledger entry used as the source of truth for statement generation.
 */
@Table("ledger_entry")
public record LedgerEntryEntity(
        @Id UUID id,
        @Column("account_id") String accountId,
        @Column("booked_at") LocalDateTime bookedAt,
        @Column("value_at") LocalDateTime valueAt,
        @Column("amount_minor") long amountMinor,
        @Column("currency") String currency,
        @Column("reference") String reference,
        @Column("description") String description,
        @Column("external_id") String externalId,
        @Column("created_at") Instant createdAt) {
}
