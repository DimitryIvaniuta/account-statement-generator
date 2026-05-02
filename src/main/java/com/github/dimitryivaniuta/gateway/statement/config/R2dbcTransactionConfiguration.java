package com.github.dimitryivaniuta.gateway.statement.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * Provides reactive transaction infrastructure for business writes done through R2DBC.
 */
@Configuration
public class R2dbcTransactionConfiguration {

    /**
     * Creates the reactive transaction manager.
     *
     * @param connectionFactory R2DBC connection factory.
     * @return reactive transaction manager.
     */
    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(final ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    /**
     * Creates a transactional operator that can wrap reactive write flows.
     *
     * @param transactionManager reactive transaction manager.
     * @return transactional operator.
     */
    @Bean
    public TransactionalOperator transactionalOperator(final ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }
}
