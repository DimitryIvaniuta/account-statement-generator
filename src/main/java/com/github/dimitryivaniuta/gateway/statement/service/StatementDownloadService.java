package com.github.dimitryivaniuta.gateway.statement.service;

import com.github.dimitryivaniuta.gateway.statement.domain.StatementArtifactEntity;
import com.github.dimitryivaniuta.gateway.statement.exception.StatementNotFoundException;
import com.github.dimitryivaniuta.gateway.statement.model.GeneratedStatement;
import com.github.dimitryivaniuta.gateway.statement.model.StatementMetadataView;
import com.github.dimitryivaniuta.gateway.statement.model.StatementRevisionView;
import com.github.dimitryivaniuta.gateway.statement.repository.StatementArtifactRepository;
import com.github.dimitryivaniuta.gateway.statement.support.YearMonthSupport;
import java.time.YearMonth;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Loads stored immutable statements for download and audit metadata access.
 */
@Service
public class StatementDownloadService {

    private final StatementArtifactRepository statementArtifactRepository;

    /**
     * Creates the download service.
     *
     * @param statementArtifactRepository statement repository.
     */
    public StatementDownloadService(final StatementArtifactRepository statementArtifactRepository) {
        this.statementArtifactRepository = statementArtifactRepository;
    }

    /**
     * Loads a stored statement artifact.
     *
     * @param accountId account identifier.
     * @param month statement month.
     * @param revision optional revision.
     * @return generated statement.
     */
    public Mono<GeneratedStatement> loadStatement(
            final String accountId,
            final YearMonth month,
            @Nullable final Integer revision) {
        return resolveArtifact(accountId, month, revision)
                .switchIfEmpty(Mono.error(new StatementNotFoundException(
                        "Statement not found for account=" + accountId + ", month=" + month
                                + (revision == null ? "" : ", revision=" + revision))))
                .map(this::toGeneratedStatement);
    }

    /**
     * Loads only the statement metadata.
     *
     * @param accountId account identifier.
     * @param month statement month.
     * @param revision optional revision.
     * @return statement metadata.
     */
    public Mono<StatementMetadataView> loadMetadata(
            final String accountId,
            final YearMonth month,
            @Nullable final Integer revision) {
        return loadStatement(accountId, month, revision).map(GeneratedStatement::metadata);
    }

    /**
     * Lists all immutable revisions for audit review.
     *
     * @param accountId account identifier.
     * @param month statement month.
     * @return revision summaries from newest to oldest.
     */
    public Flux<StatementRevisionView> loadRevisions(final String accountId, final YearMonth month) {
        return statementArtifactRepository.findAllRevisions(accountId, YearMonthSupport.toPersistedDate(month))
                .map(this::toRevisionView);
    }

    /**
     * Loads the latest revision when present without failing when missing.
     *
     * @param accountId account identifier.
     * @param month statement month.
     * @return latest statement if present.
     */
    public Mono<GeneratedStatement> loadLatestIfPresent(final String accountId, final YearMonth month) {
        return statementArtifactRepository.findLatest(accountId, YearMonthSupport.toPersistedDate(month))
                .map(this::toGeneratedStatement);
    }

    private Mono<StatementArtifactEntity> resolveArtifact(
            final String accountId,
            final YearMonth month,
            @Nullable final Integer revision) {
        return revision == null
                ? statementArtifactRepository.findLatest(accountId, YearMonthSupport.toPersistedDate(month))
                : statementArtifactRepository.findByRevision(accountId, YearMonthSupport.toPersistedDate(month), revision);
    }

    private GeneratedStatement toGeneratedStatement(final StatementArtifactEntity entity) {
        return new GeneratedStatement(toMetadata(entity), entity.payloadText());
    }

    private StatementMetadataView toMetadata(final StatementArtifactEntity entity) {
        return new StatementMetadataView(
                entity.statementId(),
                entity.accountId(),
                entity.statementMonth().toString().substring(0, 7),
                entity.revision(),
                entity.sourceChecksum(),
                entity.outputChecksum(),
                entity.generatedAt(),
                entity.entryCount(),
                entity.contentType(),
                entity.payloadSizeBytes(),
                entity.generatorVersion(),
                entity.jobExecutionId());
    }

    private StatementRevisionView toRevisionView(final StatementArtifactEntity entity) {
        return new StatementRevisionView(
                entity.statementId(),
                entity.revision(),
                entity.sourceChecksum(),
                entity.outputChecksum(),
                entity.generatedAt(),
                entity.entryCount(),
                entity.payloadSizeBytes(),
                entity.generatorVersion(),
                entity.jobExecutionId());
    }
}
