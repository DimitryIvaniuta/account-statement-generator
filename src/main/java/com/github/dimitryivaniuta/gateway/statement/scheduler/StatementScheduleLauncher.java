package com.github.dimitryivaniuta.gateway.statement.scheduler;

import com.github.dimitryivaniuta.gateway.statement.service.StatementBatchLaunchService;
import java.time.Clock;
import java.time.YearMonth;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Launches the previous month's batch job on a configurable monthly schedule.
 */
@Component
public class StatementScheduleLauncher {

    private final StatementBatchLaunchService batchLaunchService;
    private final Clock clock;

    /**
     * Creates the scheduler.
     *
     * @param batchLaunchService batch launch service.
     * @param clock application clock.
     */
    public StatementScheduleLauncher(final StatementBatchLaunchService batchLaunchService, final Clock clock) {
        this.batchLaunchService = batchLaunchService;
        this.clock = clock;
    }

    /**
     * Launches the previous month's generation job.
     */
    @Scheduled(cron = "${statements.schedule.cron:0 10 2 1 * *}", zone = "${statements.schedule.zone:UTC}")
    public void launchPreviousMonth() {
        YearMonth targetMonth = YearMonth.now(clock).minusMonths(1);
        batchLaunchService.launch(targetMonth, null).subscribe();
    }
}
