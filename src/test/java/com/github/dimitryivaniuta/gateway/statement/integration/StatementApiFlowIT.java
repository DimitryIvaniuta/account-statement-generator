package com.github.dimitryivaniuta.gateway.statement.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Lightweight smoke test that verifies the Spring application context can start in a normal build environment.
 */
@SpringBootTest
class StatementApiFlowIT {

    /**
     * Verifies the test class is loaded.
     */
    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
