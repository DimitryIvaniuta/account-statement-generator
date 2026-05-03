package com.github.dimitryivaniuta.gateway.statement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.dimitryivaniuta.gateway.statement.config.JacksonConfiguration;
import com.github.dimitryivaniuta.gateway.statement.domain.LedgerEntryEntity;
import com.github.dimitryivaniuta.gateway.statement.domain.StatementArtifactEntity;
import com.github.dimitryivaniuta.gateway.statement.domain.StatementOutboxEventEntity;
import com.github.dimitryivaniuta.gateway.statement.repository.LedgerEntryRepository;
import com.github.dimitryivaniuta.gateway.statement.repository.StatementArtifactRepository;
import com.github.dimitryivaniuta.gateway.statement.support.StatementIdFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests for {@link StatementGenerationService}.
 */
@ExtendWith(MockitoExtension.class)
class StatementGenerationServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private StatementArtifactRepository statementArtifactRepository;
    @Mock
    private SourceChecksumService sourceChecksumService;
    @Mock
    private StatementOutboxService statementOutboxService;
    @Mock
    private RedisGenerationLockService redisGenerationLockService;

    private TransactionalOperator transactionalOperator;

    @BeforeEach
    void setUp() {
        transactionalOperator = mock(TransactionalOperator.class);
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Verifies new statement creation.
     */
    @Test
    void shouldCreateNewStatementWhenSourceChecksumIsNew() {
        YearMonth month = YearMonth.of(2026, 3);
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 1, 0, 0);
        LedgerEntryEntity entry = new LedgerEntryEntity(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "ACC-1",
                LocalDateTime.of(2026, 3, 1, 10, 0),
                LocalDateTime.of(2026, 3, 1, 10, 0),
                500,
                "PLN",
                "REF-1",
                "Credit",
                "EXT-1",
                Instant.parse("2026-03-01T10:00:00Z"));
        StatementArtifactEntity saved = new StatementArtifactEntity(
                UUID.randomUUID(),
                "stmt_1",
                "ACC-1",
                LocalDate.of(2026, 3, 1),
                1,
                "src-1",
                "out-1",
                "application/json",
                "{\"statementId\":\"stmt_1\"}",
                24,
                1,
                "1.1.0",
                Instant.parse("2026-04-01T00:00:00Z"),
                "77");

        when(redisGenerationLockService.tryAcquire(eq("ACC-1"), eq(month), any())).thenReturn(Mono.just(true));
        when(redisGenerationLockService.release(eq("ACC-1"), eq(month), any())).thenReturn(Mono.empty());
        when(ledgerEntryRepository.sumBalanceBefore("ACC-1", from)).thenReturn(Mono.just(1000L));
        when(ledgerEntryRepository.findForAccountAndMonth("ACC-1", from, to)).thenReturn(Flux.just(entry));
        when(sourceChecksumService.compute(eq("ACC-1"), eq(month), eq(1000L), any())).thenReturn("src-1");
        when(statementArtifactRepository.findBySourceChecksum("ACC-1", LocalDate.of(2026, 3, 1), "src-1"))
                .thenReturn(Mono.empty());
        when(statementArtifactRepository.findMaxRevision("ACC-1", LocalDate.of(2026, 3, 1))).thenReturn(Mono.just(0));
        when(statementArtifactRepository.save(any())).thenReturn(Mono.just(saved));
        when(statementOutboxService.createStatementGeneratedEvent(saved)).thenReturn(Mono.just(new StatementOutboxEventEntity(
                UUID.randomUUID(),
                "statement_artifact",
                "stmt_1",
                "statement.generated.v1",
                "ACC-1",
                "{}",
                0,
                Instant.parse("2026-04-01T00:00:00Z"),
                null,
                null,
                null,
                Instant.parse("2026-04-01T00:00:00Z"),
                null)));

        StatementGenerationService service = new StatementGenerationService(
                ledgerEntryRepository,
                statementArtifactRepository,
                sourceChecksumService,
                new StatementPayloadFactory(),
                new CanonicalJsonStatementSerializer(new JacksonConfiguration().objectMapper()),
                statementOutboxService,
                redisGenerationLockService,
                new StatementIdFactory(),
                transactionalOperator,
                Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.UTC),
                "1.1.0");

        StepVerifier.create(service.generateForAccount("ACC-1", month, 77L))
                .assertNext(statement -> {
                    assertThat(statement.metadata().statementId()).isEqualTo("stmt_1");
                    assertThat(statement.metadata().revision()).isEqualTo(1);
                })
                .verifyComplete();
    }

    /**
     * Verifies that generation does not proceed when a lock cannot be acquired.
     */
    @Test
    void shouldReuseLatestWhenLockIsNotAcquired() {
        YearMonth month = YearMonth.of(2026, 3);
        StatementArtifactEntity existing = new StatementArtifactEntity(
                UUID.randomUUID(),
                "stmt_existing",
                "ACC-1",
                LocalDate.of(2026, 3, 1),
                2,
                "src-existing",
                "out-existing",
                "application/json",
                "{\"status\":\"existing\"}",
                21,
                3,
                "1.1.0",
                Instant.parse("2026-04-01T00:00:00Z"),
                "88");

        when(redisGenerationLockService.tryAcquire(eq("ACC-1"), eq(month), any())).thenReturn(Mono.just(false));
        when(statementArtifactRepository.findLatest("ACC-1", LocalDate.of(2026, 3, 1))).thenReturn(Mono.just(existing));

        StatementGenerationService service = new StatementGenerationService(
                ledgerEntryRepository,
                statementArtifactRepository,
                sourceChecksumService,
                new StatementPayloadFactory(),
                new CanonicalJsonStatementSerializer(new JacksonConfiguration().objectMapper()),
                statementOutboxService,
                redisGenerationLockService,
                new StatementIdFactory(),
                transactionalOperator,
                Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.UTC),
                "1.1.0");

        StepVerifier.create(service.generateForAccount("ACC-1", month, 77L))
                .assertNext(statement -> assertThat(statement.metadata().statementId()).isEqualTo("stmt_existing"))
                .verifyComplete();
    }
}
