package com.labs_101.backend.global;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import com.labs_101.backend.exception.ErrorResponse;
import com.labs_101.backend.exception.NotFoundException;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Global exception handler that catches various exceptions and provides
 * appropriate responses.
 * This class ensures that custom exceptions are handled properly and localized
 * error messages
 * are returned to the client.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    /**
     * Constructor to initialize the GlobalExceptionHandler with the MessageSource.
     *
     * @param messageSource the message source for fetching localized error
     *                      messages.
     */
    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Handles NotFoundException by returning a localized message with a NOT_FOUND
     * status.
     *
     * @param ex     the exception to be handled.
     * @param locale the locale to fetch the localized message.
     * @return a ResponseEntity with the localized message and the NOT_FOUND status.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, Locale locale,
            HttpServletRequest request) {
        String msg = messageSource.getMessage(ex.getCode(), ex.getArgs(), ex.getCode(), locale);
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                msg,
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Handles any generic exception by returning the exception message.
     * If the exception is an instance of ResponseStatusException, the status code
     * from the exception is used.
     * Otherwise, an INTERNAL_SERVER_ERROR status is returned.
     *
     * @param ex the exception to be handled.
     * @return a ResponseEntity with the exception message and appropriate status
     *         code.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        Map<String, String> response = new HashMap<>();
        response.put("message", ex.getMessage());

        if (ex instanceof ResponseStatusException e) {
            return new ResponseEntity<>(response, e.getStatusCode());
        } else {
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
