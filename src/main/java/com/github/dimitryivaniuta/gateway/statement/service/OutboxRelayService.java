package com.github.dimitryivaniuta.gateway.statement.service;

import com.github.dimitryivaniuta.gateway.statement.domain.StatementOutboxEventEntity;
import com.github.dimitryivaniuta.gateway.statement.repository.StatementOutboxEventRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Relays outbox rows to Kafka using a claim-and-publish polling pattern.
 */
@Service
public class OutboxRelayService {

    private static final Duration CLAIM_TTL = Duration.ofSeconds(30);
    private static final int MAX_ERROR_LENGTH = 1_000;

    private final StatementOutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;
    private final String topic;
    private final String workerId = UUID.randomUUID().toString();

    /**
     * Creates the relay service.
     *
     * @param outboxRepository outbox repository.
     * @param kafkaTemplate Kafka template.
     * @param clock application clock.
     * @param topic Kafka topic name.
     */
    public OutboxRelayService(
            final StatementOutboxEventRepository outboxRepository,
            final KafkaTemplate<String, String> kafkaTemplate,
            final Clock clock,
            @Value("${statements.kafka.topic:statement.generated.v1}") final String topic) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.topic = topic;
    }

    /**
     * Polls unpublished rows and relays them to Kafka.
     */
    @Scheduled(fixedDelayString = "${statements.outbox.fixed-delay:5000}")
    public void relay() {
        relayOnce().subscribe();
    }

    /**
     * Executes one relay cycle.
     *
     * @return number of rows published in this cycle.
     */
    public Mono<Integer> relayOnce() {
        Instant now = Instant.now(clock);
        return outboxRepository.findReadyBatch(now)
                .concatMap(this::claimAndPublish)
                .reduce(0, Integer::sum)
                .defaultIfEmpty(0);
    }

    private Mono<Integer> claimAndPublish(final StatementOutboxEventEntity event) {
        Instant now = Instant.now(clock);
        Instant claimExpiredBefore = now.minus(CLAIM_TTL);
        return outboxRepository.claim(event.id(), workerId, now, claimExpiredBefore)
                .flatMap(rowsUpdated -> rowsUpdated > 0 ? publishClaimed(event) : Mono.just(0));
    }

    private Mono<Integer> publishClaimed(final StatementOutboxEventEntity event) {
        return Mono.fromFuture(kafkaTemplate.send(topic, event.eventKey(), event.payloadText()))
                .then(outboxRepository.markPublished(event.id(), workerId, Instant.now(clock)))
                .map(rowsUpdated -> rowsUpdated > 0 ? 1 : 0)
                .onErrorResume(error -> handlePublishFailure(event, error).thenReturn(0));
    }

    private Mono<Void> handlePublishFailure(final StatementOutboxEventEntity event, final Throwable error) {
        int attempts = event.publishAttempts() + 1;
        Instant nextAttemptAt = Instant.now(clock).plus(computeBackoff(attempts));
        return outboxRepository.markFailed(
                        event.id(),
                        workerId,
                        attempts,
                        nextAttemptAt,
                        truncateError(error))
                .then();
    }

    private Duration computeBackoff(final int attempts) {
        long seconds = Math.min(300L, 1L << Math.min(attempts, 8));
        return Duration.ofSeconds(seconds);
    }

    private String truncateError(final Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
