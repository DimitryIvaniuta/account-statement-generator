package com.github.dimitryivaniuta.gateway.statement.exception;

/**
 * Raised when a requested statement artifact cannot be found.
 */
public class StatementNotFoundException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message exception detail.
     */
    public StatementNotFoundException(final String message) {
        super(message);
    }
}
