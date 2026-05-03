package com.github.dimitryivaniuta.gateway.statement.batch;

import com.github.dimitryivaniuta.gateway.statement.service.StatementGenerationService;
import java.time.YearMonth;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Tasklet that generates statements for a requested month and optional account filter.
 */
@Component
public class MonthlyStatementGenerationTasklet implements Tasklet {

    private final StatementGenerationService statementGenerationService;

    /**
     * Creates the tasklet.
     *
     * @param statementGenerationService statement generation service.
     */
    public MonthlyStatementGenerationTasklet(final StatementGenerationService statementGenerationService) {
        this.statementGenerationService = statementGenerationService;
    }

    /**
     * Executes the monthly generation task.
     *
     * @param contribution step contribution.
     * @param chunkContext chunk context.
     * @return finished status.
     */
    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        String statementMonthValue = (String) chunkContext.getStepContext().getJobParameters().get("statementMonth");
        String accountId = getOptionalString(chunkContext, "accountId");
        Long jobExecutionId = contribution.getStepExecution().getJobExecution().getId();
        long generatedCount = statementGenerationService
                .generateForMonth(YearMonth.parse(statementMonthValue), accountId, jobExecutionId)
                .count()
                .defaultIfEmpty(0L)
                .block();
        contribution.incrementWriteCount(Math.toIntExact(generatedCount));
        return RepeatStatus.FINISHED;
    }

    @Nullable
    private String getOptionalString(final ChunkContext chunkContext, final String key) {
        Object value = chunkContext.getStepContext().getJobParameters().get(key);
        return value == null ? null : value.toString();
    }
}
