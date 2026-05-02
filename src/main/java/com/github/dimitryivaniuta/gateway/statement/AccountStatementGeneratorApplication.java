package com.github.dimitryivaniuta.gateway.statement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bootstraps the account statement generator application.
 */
@SpringBootApplication
@EnableScheduling
public class AccountStatementGeneratorApplication {

    /**
     * Starts the application.
     *
     * @param args command-line arguments.
     */
    public static void main(final String[] args) {
        SpringApplication.run(AccountStatementGeneratorApplication.class, args);
    }
}
