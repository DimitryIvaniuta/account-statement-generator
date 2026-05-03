package com.github.dimitryivaniuta.gateway.statement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dimitryivaniuta.gateway.statement.domain.LedgerEntryEntity;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SourceChecksumService}.
 */
class SourceChecksumServiceTest {

    private final SourceChecksumService service = new SourceChecksumService();

    /**
     * Verifies that the same input produces the same checksum.
     */
    @Test
    void shouldProduceDeterministicChecksum() {
        LedgerEntryEntity entry = new LedgerEntryEntity(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "ACC-1",
                LocalDateTime.of(2026, 3, 1, 10, 0),
                LocalDateTime.of(2026, 3, 1, 10, 0),
                100,
                "PLN",
                "REF-1",
                "Salary",
                "EXT-1",
                Instant.parse("2026-03-01T10:00:00Z"));

        String first = service.compute("ACC-1", YearMonth.of(2026, 3), 1000L, List.of(entry));
        String second = service.compute("ACC-1", YearMonth.of(2026, 3), 1000L, List.of(entry));

        assertThat(first).isEqualTo(second).hasSize(64);
    }
}
