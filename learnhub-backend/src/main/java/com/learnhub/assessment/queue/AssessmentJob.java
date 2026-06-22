package com.learnhub.assessment.queue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object for assessment job messages in Redis queue.
 *
 * Serialized to JSON format for storage and transport.
 * Includes retry count and failure reason for debugging.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentJob {

    /**
     * Unique identifier of the assessment being processed.
     */
    @JsonProperty("assessment_id")
    private UUID assessmentId;

    /**
     * ID of the repository snapshot to analyze.
     */
    @JsonProperty("repo_id")
    private UUID repoId;

    /**
     * BRF (Behavior and Rule Framework) version to use for evaluation.
     * Format: "1.0.0" (semantic versioning)
     */
    @JsonProperty("brf_version")
    private String brfVersion;

    /**
     * Current retry attempt number (0 = first attempt).
     * Incremented each time job is retried.
     */
    @JsonProperty("retry_count")
    private int retryCount;

    /**
     * Reason for last failure (if any).
     * Populated by job consumer when retry is needed.
     */
    @JsonProperty("failure_reason")
    private String failureReason;

    /**
     * Create a new assessment job.
     *
     * @param assessmentId the assessment ID
     * @param repoId the repository snapshot ID
     * @param brfVersion the BRF version to use
     */
    public AssessmentJob(UUID assessmentId, UUID repoId, String brfVersion) {
        this.assessmentId = assessmentId;
        this.repoId = repoId;
        this.brfVersion = brfVersion;
        this.retryCount = 0;
        this.failureReason = null;
    }

    /**
     * Create a copy of this job with incremented retry count.
     *
     * @return new AssessmentJob with retry count incremented
     */
    public AssessmentJob withIncrementedRetry() {
        AssessmentJob copy = new AssessmentJob();
        copy.assessmentId = this.assessmentId;
        copy.repoId = this.repoId;
        copy.brfVersion = this.brfVersion;
        copy.retryCount = this.retryCount + 1;
        copy.failureReason = this.failureReason;
        return copy;
    }
}
