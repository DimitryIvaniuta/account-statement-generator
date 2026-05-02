package com.github.dimitryivaniuta.gateway.statement.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the system clock as an injectable bean to keep time-dependent code testable.
 */
@Configuration
public class ClockConfiguration {

    /**
     * Creates the application clock.
     *
     * @return UTC clock.
     */
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }
}
