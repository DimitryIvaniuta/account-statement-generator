package com.github.dimitryivaniuta.gateway.statement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dimitryivaniuta.gateway.statement.domain.LedgerEntryEntity;
import com.github.dimitryivaniuta.gateway.statement.model.StatementPayload;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StatementPayloadFactory}.
 */
class StatementPayloadFactoryTest {

    private final StatementPayloadFactory factory = new StatementPayloadFactory();

    /**
     * Verifies running balances and totals.
     */
    @Test
    void shouldBuildCorrectTotalsAndRunningBalances() {
        List<LedgerEntryEntity> entries = List.of(
                new LedgerEntryEntity(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "ACC-1",
                        LocalDateTime.of(2026, 3, 1, 10, 0),
                        LocalDateTime.of(2026, 3, 1, 10, 0),
                        500,
                        "PLN",
                        "REF-1",
                        "Credit",
                        "EXT-1",
                        Instant.parse("2026-03-01T10:00:00Z")),
                new LedgerEntryEntity(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "ACC-1",
                        LocalDateTime.of(2026, 3, 2, 11, 0),
                        LocalDateTime.of(2026, 3, 2, 11, 0),
                        -200,
                        "PLN",
                        "REF-2",
                        "Debit",
                        "EXT-2",
                        Instant.parse("2026-03-02T11:00:00Z")));

        StatementPayload payload = factory.create("stmt_1", "ACC-1", YearMonth.of(2026, 3), "src", 1000L, entries);

        assertThat(payload.totals().openingBalanceMinor()).isEqualTo(1000L);
        assertThat(payload.totals().totalCreditsMinor()).isEqualTo(500L);
        assertThat(payload.totals().totalDebitsMinor()).isEqualTo(200L);
        assertThat(payload.totals().closingBalanceMinor()).isEqualTo(1300L);
        assertThat(payload.entries()).hasSize(2);
        assertThat(payload.entries().get(0).runningBalanceMinor()).isEqualTo(1500L);
        assertThat(payload.entries().get(1).runningBalanceMinor()).isEqualTo(1300L);
    }
}
