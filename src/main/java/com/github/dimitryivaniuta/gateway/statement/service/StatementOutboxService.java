package com.github.dimitryivaniuta.gateway.statement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.statement.domain.StatementArtifactEntity;
import com.github.dimitryivaniuta.gateway.statement.domain.StatementOutboxEventEntity;
import com.github.dimitryivaniuta.gateway.statement.model.StatementGeneratedEvent;
import com.github.dimitryivaniuta.gateway.statement.repository.StatementOutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Writes immutable outbox rows after new statement artifacts are stored.
 */
@Service
public class StatementOutboxService {

    private final StatementOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Creates the outbox service.
     *
     * @param outboxRepository outbox repository.
     * @param objectMapper object mapper.
     * @param clock application clock.
     */
    public StatementOutboxService(
            final StatementOutboxEventRepository outboxRepository,
            final ObjectMapper objectMapper,
            final Clock clock) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Writes a statement-generated outbox row.
     *
     * @param artifact stored artifact.
     * @return saved outbox row.
     */
    public Mono<StatementOutboxEventEntity> createStatementGeneratedEvent(final StatementArtifactEntity artifact) {
        StatementGeneratedEvent event = new StatementGeneratedEvent(
                artifact.statementId(),
                artifact.accountId(),
                artifact.statementMonth().toString().substring(0, 7),
                artifact.revision(),
                artifact.sourceChecksum(),
                artifact.outputChecksum(),
                artifact.contentType(),
                artifact.payloadSizeBytes(),
                artifact.generatedAt());
        Instant now = Instant.now(clock);
        StatementOutboxEventEntity entity = new StatementOutboxEventEntity(
                UUID.randomUUID(),
                "statement_artifact",
                artifact.statementId(),
                "statement.generated.v1",
                artifact.accountId(),
                serialize(event),
                0,
                now,
                null,
                null,
                null,
                now,
                null);
        return outboxRepository.save(entity);
    }

    private String serialize(final StatementGeneratedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize statement-generated event", exception);
        }
    }
}
