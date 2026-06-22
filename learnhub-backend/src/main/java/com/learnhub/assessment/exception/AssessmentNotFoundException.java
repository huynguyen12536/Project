package com.learnhub.assessment.exception;

/**
 * Exception thrown when an assessment is not found or not owned by the user.
 */
public class AssessmentNotFoundException extends RuntimeException {

    public AssessmentNotFoundException(String message) {
        super(message);
    }
}
