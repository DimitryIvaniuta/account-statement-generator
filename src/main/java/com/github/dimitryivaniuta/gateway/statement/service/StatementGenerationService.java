package com.github.dimitryivaniuta.gateway.statement.service;

import com.github.dimitryivaniuta.gateway.statement.domain.LedgerEntryEntity;
import com.github.dimitryivaniuta.gateway.statement.domain.StatementArtifactEntity;
import com.github.dimitryivaniuta.gateway.statement.model.GeneratedStatement;
import com.github.dimitryivaniuta.gateway.statement.model.StatementMetadataView;
import com.github.dimitryivaniuta.gateway.statement.model.StatementPayload;
import com.github.dimitryivaniuta.gateway.statement.repository.LedgerEntryRepository;
import com.github.dimitryivaniuta.gateway.statement.repository.StatementArtifactRepository;
import com.github.dimitryivaniuta.gateway.statement.support.Sha256Support;
import com.github.dimitryivaniuta.gateway.statement.support.StatementIdFactory;
import com.github.dimitryivaniuta.gateway.statement.support.YearMonthSupport;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Generates immutable monthly statements from ledger entries.
 */
@Service
public class StatementGenerationService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final StatementArtifactRepository statementArtifactRepository;
    private final SourceChecksumService sourceChecksumService;
    private final StatementPayloadFactory statementPayloadFactory;
    private final CanonicalJsonStatementSerializer statementSerializer;
    private final StatementOutboxService statementOutboxService;
    private final RedisGenerationLockService redisGenerationLockService;
    private final StatementIdFactory statementIdFactory;
    private final TransactionalOperator transactionalOperator;
    private final Clock clock;
    private final String generatorVersion;

    /**
     * Creates the generation service.
     *
     * @param ledgerEntryRepository ledger repository.
     * @param statementArtifactRepository statement repository.
     * @param sourceChecksumService checksum service.
     * @param statementPayloadFactory payload factory.
     * @param statementSerializer canonical serializer.
     * @param statementOutboxService outbox service.
     * @param redisGenerationLockService Redis lock service.
     * @param statementIdFactory statement identifier factory.
     * @param transactionalOperator reactive transaction operator.
     * @param clock application clock.
     * @param generatorVersion generator version.
     */
    public StatementGenerationService(
            final LedgerEntryRepository ledgerEntryRepository,
            final StatementArtifactRepository statementArtifactRepository,
            final SourceChecksumService sourceChecksumService,
            final StatementPayloadFactory statementPayloadFactory,
            final CanonicalJsonStatementSerializer statementSerializer,
            final StatementOutboxService statementOutboxService,
            final RedisGenerationLockService redisGenerationLockService,
            final StatementIdFactory statementIdFactory,
            final TransactionalOperator transactionalOperator,
            final Clock clock,
            @Value("${statements.generator-version:1.1.0}") final String generatorVersion) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.statementArtifactRepository = statementArtifactRepository;
        this.sourceChecksumService = sourceChecksumService;
        this.statementPayloadFactory = statementPayloadFactory;
        this.statementSerializer = statementSerializer;
        this.statementOutboxService = statementOutboxService;
        this.redisGenerationLockService = redisGenerationLockService;
        this.statementIdFactory = statementIdFactory;
        this.transactionalOperator = transactionalOperator;
        this.clock = clock;
        this.generatorVersion = generatorVersion;
    }

    /**
     * Generates statements for a month.
     *
     * @param month target month.
     * @param accountId optional single-account filter.
     * @param jobExecutionId optional batch execution identifier.
     * @return generated or reused statements.
     */
    public Flux<GeneratedStatement> generateForMonth(
            final YearMonth month,
            @Nullable final String accountId,
            @Nullable final Long jobExecutionId) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        Flux<String> accounts = accountId == null || accountId.isBlank()
                ? ledgerEntryRepository.findDistinctAccountIdsForMonth(from, to)
                : Flux.just(accountId);
        return accounts.concatMap(currentAccountId -> generateForAccount(currentAccountId, month, jobExecutionId));
    }

    /**
     * Generates a statement for one account.
     *
     * @param accountId account identifier.
     * @param month target month.
     * @param jobExecutionId optional batch execution identifier.
     * @return generated or existing statement.
     */
    public Mono<GeneratedStatement> generateForAccount(
            final String accountId,
            final YearMonth month,
            @Nullable final Long jobExecutionId) {
        String token = UUID.randomUUID().toString();
        return redisGenerationLockService.tryAcquire(accountId, month, token)
                .flatMap(acquired -> acquired
                        ? withLock(accountId, month, token, jobExecutionId)
                        : loadLatestIfPresent(accountId, month));
    }

    private Mono<GeneratedStatement> withLock(
            final String accountId,
            final YearMonth month,
            final String token,
            @Nullable final Long jobExecutionId) {
        return doGenerate(accountId, month, jobExecutionId)
                .flatMap(result -> redisGenerationLockService.release(accountId, month, token).thenReturn(result))
                .switchIfEmpty(redisGenerationLockService.release(accountId, month, token).then(Mono.empty()))
                .onErrorResume(error -> redisGenerationLockService.release(accountId, month, token)
                        .then(Mono.error(error)));
    }

    private Mono<GeneratedStatement> loadLatestIfPresent(final String accountId, final YearMonth month) {
        return statementArtifactRepository.findLatest(accountId, YearMonthSupport.toPersistedDate(month))
                .map(this::toGeneratedStatement);
    }

    private Mono<GeneratedStatement> doGenerate(
            final String accountId,
            final YearMonth month,
            @Nullable final Long jobExecutionId) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        return Mono.zip(
                        ledgerEntryRepository.sumBalanceBefore(accountId, from).defaultIfEmpty(0L),
                        ledgerEntryRepository.findForAccountAndMonth(accountId, from, to).collectList())
                .flatMap(tuple -> createOrLoadStatement(accountId, month, tuple.getT1(), tuple.getT2(), jobExecutionId));
    }

    private Mono<GeneratedStatement> createOrLoadStatement(
            final String accountId,
            final YearMonth month,
            final long openingBalanceMinor,
            final List<LedgerEntryEntity> entries,
            @Nullable final Long jobExecutionId) {
        if (entries.isEmpty()) {
            return Mono.empty();
        }

        String sourceChecksum = sourceChecksumService.compute(accountId, month, openingBalanceMinor, entries);
        return statementArtifactRepository.findBySourceChecksum(accountId, YearMonthSupport.toPersistedDate(month), sourceChecksum)
                .map(this::toGeneratedStatement)
                .switchIfEmpty(Mono.defer(() -> createNewStatement(
                        accountId,
                        month,
                        openingBalanceMinor,
                        entries,
                        sourceChecksum,
                        jobExecutionId)));
    }

    private Mono<GeneratedStatement> createNewStatement(
            final String accountId,
            final YearMonth month,
            final long openingBalanceMinor,
            final List<LedgerEntryEntity> entries,
            final String sourceChecksum,
            @Nullable final Long jobExecutionId) {
        return statementArtifactRepository.findMaxRevision(accountId, YearMonthSupport.toPersistedDate(month))
                .defaultIfEmpty(0)
                .map(maxRevision -> maxRevision + 1)
                .flatMap(revision -> saveNewStatement(
                        accountId,
                        month,
                        openingBalanceMinor,
                        entries,
                        sourceChecksum,
                        revision,
                        jobExecutionId))
                .onErrorResume(DuplicateKeyException.class,
                        duplicate -> statementArtifactRepository.findBySourceChecksum(
                                        accountId,
                                        YearMonthSupport.toPersistedDate(month),
                                        sourceChecksum)
                                .map(this::toGeneratedStatement));
    }

    private Mono<GeneratedStatement> saveNewStatement(
            final String accountId,
            final YearMonth month,
            final long openingBalanceMinor,
            final List<LedgerEntryEntity> entries,
            final String sourceChecksum,
            final int revision,
            @Nullable final Long jobExecutionId) {
        String statementId = statementIdFactory.create(accountId, month, sourceChecksum);
        StatementPayload payload = statementPayloadFactory.create(
                statementId,
                accountId,
                month,
                sourceChecksum,
                openingBalanceMinor,
                entries);
        String payloadText = statementSerializer.serialize(payload);
        byte[] payloadBytes = payloadText.getBytes(StandardCharsets.UTF_8);
        String outputChecksum = Sha256Support.hex(payloadBytes);
        StatementArtifactEntity entity = new StatementArtifactEntity(
                UUID.randomUUID(),
                statementId,
                accountId,
                YearMonthSupport.toPersistedDate(month),
                revision,
                sourceChecksum,
                outputChecksum,
                "application/json",
                payloadText,
                payloadBytes.length,
                entries.size(),
                generatorVersion,
                Instant.now(clock),
                jobExecutionId == null ? null : String.valueOf(jobExecutionId));
        return transactionalOperator.transactional(
                        statementArtifactRepository.save(entity)
                                .flatMap(saved -> statementOutboxService.createStatementGeneratedEvent(saved)
                                        .thenReturn(saved)))
                .map(this::toGeneratedStatement);
    }

    private GeneratedStatement toGeneratedStatement(final StatementArtifactEntity entity) {
        StatementMetadataView metadata = new StatementMetadataView(
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
        return new GeneratedStatement(metadata, entity.payloadText());
    }
}
