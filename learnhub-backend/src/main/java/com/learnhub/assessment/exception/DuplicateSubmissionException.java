package com.learnhub.assessment.exception;

/**
 * Exception thrown when a duplicate assessment submission is detected.
 * Occurs when the same user submits an assessment for the same snapshot
 * within the 60-second duplicate protection window.
 *
 * HTTP Status: 409 Conflict
 */
public class DuplicateSubmissionException extends RuntimeException {

    private final int retryAfterSeconds;

    /**
     * Create exception with message and retry-after duration.
     *
     * @param message descriptive error message
     * @param retryAfterSeconds seconds to wait before retry
     */
    public DuplicateSubmissionException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Create exception with message only (default 60 second retry).
     *
     * @param message descriptive error message
     */
    public DuplicateSubmissionException(String message) {
        super(message);
        this.retryAfterSeconds = 60;
    }

    /**
     * Get recommended retry-after duration in seconds.
     *
     * @return seconds to wait before retry
     */
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
