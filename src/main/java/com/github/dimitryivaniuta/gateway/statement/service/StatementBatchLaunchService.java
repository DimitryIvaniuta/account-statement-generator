package com.github.dimitryivaniuta.gateway.statement.service;

import java.time.Clock;
import java.time.YearMonth;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Launches the Spring Batch job that generates monthly statements.
 */
@Service
public class StatementBatchLaunchService {

    private final JobLauncher jobLauncher;
    private final Job monthlyStatementJob;
    private final Clock clock;

    /**
     * Creates the launcher service.
     *
     * @param jobLauncher Spring Batch launcher.
     * @param monthlyStatementJob monthly statement job.
     * @param clock application clock.
     */
    public StatementBatchLaunchService(
            final JobLauncher jobLauncher,
            final Job monthlyStatementJob,
            final Clock clock) {
        this.jobLauncher = jobLauncher;
        this.monthlyStatementJob = monthlyStatementJob;
        this.clock = clock;
    }

    /**
     * Launches the job for a month and optional single-account filter.
     *
     * @param month target month.
     * @param accountId optional single-account filter.
     * @return launched execution identifier.
     */
    public Mono<Long> launch(final YearMonth month, @Nullable final String accountId) {
        return Mono.fromCallable(() -> {
                    JobExecution execution = jobLauncher.run(monthlyStatementJob, buildParameters(month, accountId));
                    return execution.getId();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private JobParameters buildParameters(final YearMonth month, @Nullable final String accountId) {
        JobParametersBuilder builder = new JobParametersBuilder()
                .addString("statementMonth", month.toString())
                .addLong("requestedAt", clock.millis());
        if (accountId != null && !accountId.isBlank()) {
            builder.addString("accountId", accountId);
        }
        return builder.toJobParameters();
    }
}
