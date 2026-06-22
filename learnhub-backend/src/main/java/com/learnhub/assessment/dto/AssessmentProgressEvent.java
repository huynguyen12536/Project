package com.learnhub.assessment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learnhub.assessment.entity.AssessmentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Real-time progress event emitted during assessment processing.
 * Represents the current state of an assessment as it moves through the pipeline.
 * Emitted via SSE every 1-2 seconds or retrieved via polling fallback.
 */
public record AssessmentProgressEvent(
    @JsonProperty("assessmentId")
    UUID assessmentId,

    @JsonProperty("status")
    AssessmentStatus status,

    @JsonProperty("progressPercent")
    Integer progressPercent,

    @JsonProperty("currentStep")
    String currentStep,

    @JsonProperty("estimatedSecondsRemaining")
    Integer estimatedSecondsRemaining,

    @JsonProperty("queuePosition")
    Integer queuePosition,

    @JsonProperty("confidence")
    Double confidence,

    @JsonProperty("timestamp")
    Instant timestamp
) {
    /**
     * Validates event invariants.
     */
    public AssessmentProgressEvent {
        if (assessmentId == null) {
            throw new IllegalArgumentException("assessmentId cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        if (progressPercent == null || progressPercent < 0 || progressPercent > 100) {
            throw new IllegalArgumentException("progressPercent must be between 0 and 100");
        }
        if (currentStep == null || currentStep.isEmpty()) {
            throw new IllegalArgumentException("currentStep cannot be null or empty");
        }
        if (confidence == null || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp cannot be null");
        }
    }
}
