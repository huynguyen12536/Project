package com.learnhub.assessment.exception;

/**
 * Exception thrown when a user exceeds the maximum number of concurrent active assessments.
 * Per the rate limiting policy, a user can have at most 3 concurrent PROCESSING assessments.
 *
 * HTTP Status: 409 Conflict
 */
public class TooManyActiveAssessmentsException extends RuntimeException {

    private static final int MAX_CONCURRENT_ACTIVE = 3;

    /**
     * Create exception with message.
     *
     * @param message descriptive error message
     */
    public TooManyActiveAssessmentsException(String message) {
        super(message);
    }

    /**
     * Get the maximum allowed concurrent active assessments.
     *
     * @return maximum concurrent active assessments per user
     */
    public int getMaxConcurrentActive() {
        return MAX_CONCURRENT_ACTIVE;
    }
}
