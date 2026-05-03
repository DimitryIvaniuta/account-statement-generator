package com.github.dimitryivaniuta.gateway.statement.support;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Helper methods for month conversions.
 */
public final class YearMonthSupport {

    private YearMonthSupport() {
    }

    /**
     * Converts a month to the first day representation used in the database.
     *
     * @param yearMonth month.
     * @return first day of month.
     */
    public static LocalDate toPersistedDate(final YearMonth yearMonth) {
        return yearMonth.atDay(1);
    }
}
