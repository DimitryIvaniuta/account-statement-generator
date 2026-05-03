package com.github.dimitryivaniuta.gateway.statement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.dimitryivaniuta.gateway.statement.domain.StatementArtifactEntity;
import com.github.dimitryivaniuta.gateway.statement.exception.StatementNotFoundException;
import com.github.dimitryivaniuta.gateway.statement.repository.StatementArtifactRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests for {@link StatementDownloadService}.
 */
@ExtendWith(MockitoExtension.class)
class StatementDownloadServiceTest {

    @Mock
    private StatementArtifactRepository repository;

    /**
     * Verifies latest revision retrieval.
     */
    @Test
    void shouldLoadLatestStatement() {
        StatementArtifactEntity entity = new StatementArtifactEntity(
                UUID.randomUUID(),
                "stmt_1",
                "ACC-1",
                LocalDate.of(2026, 3, 1),
                2,
                "src",
                "out",
                "application/json",
                "{\"ok\":true}",
                11,
                3,
                "1.1.0",
                Instant.parse("2026-04-01T00:00:00Z"),
                "10");
        when(repository.findLatest("ACC-1", LocalDate.of(2026, 3, 1))).thenReturn(Mono.just(entity));

        StatementDownloadService service = new StatementDownloadService(repository);

        StepVerifier.create(service.loadStatement("ACC-1", YearMonth.of(2026, 3), null))
                .assertNext(statement -> {
                    assertThat(statement.metadata().revision()).isEqualTo(2);
                    assertThat(statement.metadata().payloadSizeBytes()).isEqualTo(11);
                    assertThat(statement.payloadText()).contains("ok");
                })
                .verifyComplete();
    }

    /**
     * Verifies revision history retrieval.
     */
    @Test
    void shouldLoadRevisionHistory() {
        StatementArtifactEntity latest = new StatementArtifactEntity(
                UUID.randomUUID(),
                "stmt_2",
                "ACC-1",
                LocalDate.of(2026, 3, 1),
                2,
                "src-2",
                "out-2",
                "application/json",
                "{}",
                2,
                3,
                "1.1.0",
                Instant.parse("2026-04-02T00:00:00Z"),
                "11");
        StatementArtifactEntity older = new StatementArtifactEntity(
                UUID.randomUUID(),
                "stmt_1",
                "ACC-1",
                LocalDate.of(2026, 3, 1),
                1,
                "src-1",
                "out-1",
                "application/json",
                "{}",
                2,
                3,
                "1.0.0",
                Instant.parse("2026-04-01T00:00:00Z"),
                "10");
        when(repository.findAllRevisions("ACC-1", LocalDate.of(2026, 3, 1)))
                .thenReturn(Flux.just(latest, older));

        StatementDownloadService service = new StatementDownloadService(repository);

        StepVerifier.create(service.loadRevisions("ACC-1", YearMonth.of(2026, 3)).collectList())
                .assertNext(revisions -> {
                    assertThat(revisions).hasSize(2);
                    assertThat(revisions.get(0).revision()).isEqualTo(2);
                    assertThat(revisions.get(1).revision()).isEqualTo(1);
                })
                .verifyComplete();
    }

    /**
     * Verifies not-found behavior.
     */
    @Test
    void shouldFailWhenStatementMissing() {
        when(repository.findLatest("ACC-1", LocalDate.of(2026, 3, 1))).thenReturn(Mono.empty());

        StatementDownloadService service = new StatementDownloadService(repository);

        StepVerifier.create(service.loadStatement("ACC-1", YearMonth.of(2026, 3), null))
                .expectError(StatementNotFoundException.class)
                .verify();
    }
}
