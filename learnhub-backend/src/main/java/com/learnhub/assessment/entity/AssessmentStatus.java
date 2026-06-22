package com.learnhub.assessment.entity;

/**
 * Enum representing the status of an assessment submission.
 * Transitions: PENDING → PROCESSING → COMPLETED/FAILED
 */
public enum AssessmentStatus {
    PENDING("Submitted, awaiting analysis"),
    PROCESSING("Analysis in progress"),
    COMPLETED("Analysis completed successfully"),
    FAILED("Analysis failed");

    private final String description;

    AssessmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
