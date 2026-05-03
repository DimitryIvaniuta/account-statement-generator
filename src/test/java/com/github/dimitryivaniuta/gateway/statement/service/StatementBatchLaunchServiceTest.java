package com.github.dimitryivaniuta.gateway.statement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import reactor.test.StepVerifier;

/**
 * Tests for {@link StatementBatchLaunchService}.
 */
@ExtendWith(MockitoExtension.class)
class StatementBatchLaunchServiceTest {

    @Mock
    private JobLauncher jobLauncher;
    @Mock
    private Job job;

    /**
     * Verifies job launch result mapping.
     */
    @Test
    void shouldReturnExecutionId() throws Exception {
        JobExecution execution = mock(JobExecution.class);
        when(execution.getId()).thenReturn(42L);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(execution);

        StatementBatchLaunchService service = new StatementBatchLaunchService(
                jobLauncher,
                job,
                Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.UTC));

        StepVerifier.create(service.launch(YearMonth.of(2026, 3), "ACC-1"))
                .assertNext(id -> assertThat(id).isEqualTo(42L))
                .verifyComplete();
    }
}
