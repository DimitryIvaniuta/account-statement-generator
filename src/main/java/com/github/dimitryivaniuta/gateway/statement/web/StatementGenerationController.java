package com.github.dimitryivaniuta.gateway.statement.web;

import com.github.dimitryivaniuta.gateway.statement.service.StatementBatchLaunchService;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Exposes manual batch-trigger endpoints for statement generation.
 */
@RestController
@RequestMapping("/api/v1/statements/generation")
public class StatementGenerationController {

    private final StatementBatchLaunchService batchLaunchService;

    /**
     * Creates the controller.
     *
     * @param batchLaunchService batch launch service.
     */
    public StatementGenerationController(final StatementBatchLaunchService batchLaunchService) {
        this.batchLaunchService = batchLaunchService;
    }

    /**
     * Triggers monthly statement generation.
     *
     * @param month target month.
     * @param accountId optional single-account filter.
     * @return accepted response with job execution identifier.
     */
    @PostMapping("/{month}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Map<String, Object>> generate(
            @PathVariable final String month,
            @RequestParam(required = false) final String accountId) {
        YearMonth yearMonth = YearMonth.parse(month);
        return batchLaunchService.launch(yearMonth, accountId)
                .map(executionId -> Map.<String, Object>of(
                        "status", "ACCEPTED",
                        "statementMonth", month,
                        "accountId", accountId == null ? "ALL" : accountId,
                        "jobExecutionId", executionId));
    }
}
