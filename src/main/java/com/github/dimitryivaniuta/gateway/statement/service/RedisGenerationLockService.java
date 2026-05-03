package com.github.dimitryivaniuta.gateway.statement.service;

import java.time.Duration;
import java.time.YearMonth;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Provides a best-effort Redis lock to reduce duplicate concurrent statement generation.
 *
 * <p>The lock is an optimization only. PostgreSQL uniqueness constraints remain the authoritative
 * idempotency guard.
 */
@Service
public class RedisGenerationLockService {

    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * Creates the lock service.
     *
     * @param redisTemplate reactive Redis template.
     */
    public RedisGenerationLockService(final ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to acquire a lock.
     *
     * @param accountId account identifier.
     * @param month month being generated.
     * @param token caller token.
     * @return true when the lock was acquired.
     */
    public Mono<Boolean> tryAcquire(final String accountId, final YearMonth month, final String token) {
        return redisTemplate.opsForValue().setIfAbsent(lockKey(accountId, month), token, LOCK_TTL)
                .defaultIfEmpty(false);
    }

    /**
     * Releases a lock when the same token still owns it.
     *
     * @param accountId account identifier.
     * @param month month being generated.
     * @param token caller token.
     * @return completion signal.
     */
    public Mono<Void> release(final String accountId, final YearMonth month, final String token) {
        String key = lockKey(accountId, month);
        return redisTemplate.opsForValue().get(key)
                .filter(token::equals)
                .flatMap(current -> redisTemplate.delete(key).then())
                .then();
    }

    private String lockKey(final String accountId, final YearMonth month) {
        return "statements:lock:" + accountId + ':' + month;
    }
}
