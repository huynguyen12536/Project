package com.learnhub.brf.exception;

/**
 * Exception thrown when a requested BRF version is not found.
 */
public class BrfVersionNotFoundException extends RuntimeException {

    /**
     * Create exception with message.
     *
     * @param message descriptive error message
     */
    public BrfVersionNotFoundException(String message) {
        super(message);
    }

    /**
     * Create exception with message and cause.
     *
     * @param message descriptive error message
     * @param cause the underlying exception
     */
    public BrfVersionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
