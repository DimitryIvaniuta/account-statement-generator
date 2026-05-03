package com.github.dimitryivaniuta.gateway.statement.config;

import com.github.dimitryivaniuta.gateway.statement.batch.MonthlyStatementGenerationTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configures the Spring Batch job that orchestrates monthly statement generation.
 */
@Configuration
public class BatchConfiguration {

    /**
     * Creates the transaction manager for the tasklet step.
     *
     * @return resourceless transaction manager.
     */
    @Bean
    public PlatformTransactionManager batchTransactionManager() {
        return new ResourcelessTransactionManager();
    }

    /**
     * Creates the generation step.
     *
     * @param jobRepository job repository.
     * @param batchTransactionManager transaction manager.
     * @param tasklet statement generation tasklet.
     * @return statement generation step.
     */
    @Bean
    public Step generateMonthlyStatementsStep(
            final JobRepository jobRepository,
            final PlatformTransactionManager batchTransactionManager,
            final MonthlyStatementGenerationTasklet tasklet) {
        return new StepBuilder("generateMonthlyStatementsStep", jobRepository)
                .tasklet(tasklet, batchTransactionManager)
                .build();
    }

    /**
     * Creates the monthly statement job.
     *
     * @param jobRepository job repository.
     * @param generateMonthlyStatementsStep single job step.
     * @return monthly statement job.
     */
    @Bean
    public Job monthlyStatementJob(final JobRepository jobRepository, final Step generateMonthlyStatementsStep) {
        return new JobBuilder("monthlyStatementJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(generateMonthlyStatementsStep)
                .build();
    }
}
