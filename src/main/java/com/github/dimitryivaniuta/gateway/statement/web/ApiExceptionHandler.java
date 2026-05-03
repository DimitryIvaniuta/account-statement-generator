package com.github.dimitryivaniuta.gateway.statement.web;

import com.github.dimitryivaniuta.gateway.statement.exception.StatementNotFoundException;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps common exceptions to RFC 9457 problem details.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Converts a not-found exception into a problem detail response.
     *
     * @param exception not-found exception.
     * @return problem detail.
     */
    @ExceptionHandler(StatementNotFoundException.class)
    public ProblemDetail handleNotFound(final StatementNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /**
     * Converts month parsing errors into a problem detail response.
     *
     * @param exception parsing exception.
     * @return problem detail.
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ProblemDetail handleDateTimeParse(final DateTimeParseException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Month must use yyyy-MM format");
    }

    /**
     * Converts input validation failures into a problem detail response.
     *
     * @param exception validation exception.
     * @return problem detail.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(final IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
