package com.github.dimitryivaniuta.gateway.statement.web;

import com.github.dimitryivaniuta.gateway.statement.model.GeneratedStatement;
import com.github.dimitryivaniuta.gateway.statement.model.StatementMetadataView;
import com.github.dimitryivaniuta.gateway.statement.model.StatementRevisionView;
import com.github.dimitryivaniuta.gateway.statement.service.StatementDownloadService;
import java.time.YearMonth;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Exposes download and audit metadata endpoints for immutable statements.
 */
@RestController
@RequestMapping("/api/v1/statements")
public class StatementQueryController {

    private final StatementDownloadService statementDownloadService;

    /**
     * Creates the controller.
     *
     * @param statementDownloadService download service.
     */
    public StatementQueryController(final StatementDownloadService statementDownloadService) {
        this.statementDownloadService = statementDownloadService;
    }

    /**
     * Downloads the stored statement artifact.
     *
     * @param month statement month.
     * @param accountId account identifier.
     * @param revision optional revision filter.
     * @param ifNoneMatch optional conditional request header.
     * @return JSON statement artifact or 304 when the artifact already matches the client ETag.
     */
    @GetMapping(value = "/{month}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> downloadStatement(
            @PathVariable final String month,
            @RequestParam final String accountId,
            @RequestParam(required = false) final Integer revision,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) final String ifNoneMatch) {
        YearMonth yearMonth = YearMonth.parse(month);
        return statementDownloadService.loadStatement(accountId, yearMonth, revision)
                .map(statement -> toDownloadResponse(statement, ifNoneMatch));
    }

    /**
     * Returns only the stored audit metadata.
     *
     * @param month statement month.
     * @param accountId account identifier.
     * @param revision optional revision.
     * @return metadata view.
     */
    @GetMapping("/{month}/metadata")
    public Mono<StatementMetadataView> metadata(
            @PathVariable final String month,
            @RequestParam final String accountId,
            @RequestParam(required = false) final Integer revision) {
        return statementDownloadService.loadMetadata(accountId, YearMonth.parse(month), revision);
    }

    /**
     * Lists immutable revision history for audit review.
     *
     * @param month statement month.
     * @param accountId account identifier.
     * @return revision summaries from newest to oldest.
     */
    @GetMapping("/{month}/revisions")
    public Flux<StatementRevisionView> revisions(
            @PathVariable final String month,
            @RequestParam final String accountId) {
        return statementDownloadService.loadRevisions(accountId, YearMonth.parse(month));
    }

    private ResponseEntity<String> toDownloadResponse(
            final GeneratedStatement statement,
            final String ifNoneMatch) {
        String etag = '"' + statement.metadata().outputChecksum() + '"';
        if (etagMatches(ifNoneMatch, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(statement.metadata().outputChecksum())
                    .lastModified(statement.metadata().generatedAt().toEpochMilli())
                    .header(HttpHeaders.CACHE_CONTROL, "private, no-cache")
                    .build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(statement.metadata().contentType()))
                .contentLength(statement.metadata().payloadSizeBytes())
                .eTag(statement.metadata().outputChecksum())
                .lastModified(statement.metadata().generatedAt().toEpochMilli())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-cache")
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(statement))
                .header("X-Statement-Id", statement.metadata().statementId())
                .header("X-Statement-Revision", String.valueOf(statement.metadata().revision()))
                .header("X-Statement-Source-Checksum", statement.metadata().sourceChecksum())
                .header("X-Statement-Output-Checksum", statement.metadata().outputChecksum())
                .header("X-Statement-Generator-Version", statement.metadata().generatorVersion())
                .header("X-Statement-Entry-Count", String.valueOf(statement.metadata().entryCount()))
                .header("X-Statement-Job-Execution-Id", nullSafe(statement.metadata().jobExecutionId()))
                .body(statement.payloadText());
    }

    private boolean etagMatches(final String ifNoneMatch, final String currentEtag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
            return false;
        }
        for (String candidate : ifNoneMatch.split(",")) {
            String normalized = candidate.trim();
            if ("*".equals(normalized) || currentEtag.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String buildContentDisposition(final GeneratedStatement statement) {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(statement.metadata().accountId() + '-' + statement.metadata().statementMonth()
                        + "-r" + statement.metadata().revision() + ".json")
                .build();
        return contentDisposition.toString();
    }

    private String nullSafe(final String value) {
        return value == null ? "" : value;
    }
}
