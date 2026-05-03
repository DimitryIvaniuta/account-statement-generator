package com.github.dimitryivaniuta.gateway.statement.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.dimitryivaniuta.gateway.statement.model.GeneratedStatement;
import com.github.dimitryivaniuta.gateway.statement.model.StatementMetadataView;
import com.github.dimitryivaniuta.gateway.statement.model.StatementRevisionView;
import com.github.dimitryivaniuta.gateway.statement.service.StatementDownloadService;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tests for {@link StatementQueryController}.
 */
@ExtendWith(MockitoExtension.class)
class StatementQueryControllerTest {

    @Mock
    private StatementDownloadService statementDownloadService;

    /**
     * Verifies attachment headers on download.
     */
    @Test
    void shouldReturnAttachmentResponse() {
        StatementMetadataView metadata = new StatementMetadataView(
                "stmt_1",
                "ACC-1",
                "2026-03",
                2,
                "src",
                "out",
                Instant.parse("2026-04-01T00:00:00Z"),
                3,
                "application/json",
                11,
                "1.1.0",
                "100");
        when(statementDownloadService.loadStatement("ACC-1", YearMonth.of(2026, 3), 2))
                .thenReturn(Mono.just(new GeneratedStatement(metadata, "{\"ok\":true}")));

        StatementQueryController controller = new StatementQueryController(statementDownloadService);
        ResponseEntity<String> response = controller.downloadStatement("2026-03", "ACC-1", 2, null).block();

        assertThat(response).isNotNull();
        assertThat(response.getHeaders().getETag()).isEqualTo('"' + metadata.outputChecksum() + '"');
        assertThat(response.getHeaders().getFirst("X-Statement-Id")).isEqualTo("stmt_1");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("ACC-1-2026-03-r2.json");
        assertThat(response.getBody()).contains("ok");
    }

    /**
     * Verifies conditional GET support.
     */
    @Test
    void shouldReturnNotModifiedWhenEtagMatches() {
        StatementMetadataView metadata = new StatementMetadataView(
                "stmt_1",
                "ACC-1",
                "2026-03",
                2,
                "src",
                "out",
                Instant.parse("2026-04-01T00:00:00Z"),
                3,
                "application/json",
                11,
                "1.1.0",
                "100");
        when(statementDownloadService.loadStatement("ACC-1", YearMonth.of(2026, 3), 2))
                .thenReturn(Mono.just(new GeneratedStatement(metadata, "{\"ok\":true}")));

        StatementQueryController controller = new StatementQueryController(statementDownloadService);
        ResponseEntity<String> response = controller.downloadStatement(
                "2026-03",
                "ACC-1",
                2,
                '"' + metadata.outputChecksum() + '"').block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
        assertThat(response.getBody()).isNull();
    }

    /**
     * Verifies revision history endpoint wiring.
     */
    @Test
    void shouldReturnRevisionHistory() {
        when(statementDownloadService.loadRevisions("ACC-1", YearMonth.of(2026, 3)))
                .thenReturn(Flux.just(
                        new StatementRevisionView("stmt_2", 2, "src-2", "out-2", Instant.parse("2026-04-02T00:00:00Z"), 3, 12, "1.1.0", "11"),
                        new StatementRevisionView("stmt_1", 1, "src-1", "out-1", Instant.parse("2026-04-01T00:00:00Z"), 3, 12, "1.0.0", "10")));

        StatementQueryController controller = new StatementQueryController(statementDownloadService);
        List<StatementRevisionView> revisions = controller.revisions("2026-03", "ACC-1").collectList().block();

        assertThat(revisions).hasSize(2);
        assertThat(revisions.get(0).revision()).isEqualTo(2);
    }
}
