package com.learnhub.assessment.service;

import com.learnhub.assessment.dto.AssessmentProgressEvent;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Service for computing real-time progress events during assessment processing.
 *
 * Computes progress state based on assessment status, elapsed time, and estimated duration.
 * Emitted every 1-2 seconds via SSE or retrieved via polling fallback.
 *
 * MVP Implementation:
 * - Mock progress tracking (simplified % calculation based on elapsed time)
 * - Queue position mocked as random value
 * - Estimated time calculated from elapsed duration
 */
@Service
@Slf4j
public class ProgressTracker {

    private static final long ESTIMATED_TOTAL_DURATION_SECONDS = 60L; // MVP estimate
    private static final String STEP_PENDING = "Waiting in queue...";
    private static final String STEP_PROCESSING = "Analyzing repository structure...";
    private static final String STEP_COMPLETED = "Analysis completed successfully.";
    private static final String STEP_FAILED = "Analysis failed due to an error.";

    /**
     * Compute progress event for an assessment.
     *
     * @param assessment the assessment entity
     * @return AssessmentProgressEvent with current progress state
     * @throws IllegalArgumentException if assessment is null
     */
    public AssessmentProgressEvent computeProgress(Assessment assessment) {
        if (assessment == null) {
            throw new IllegalArgumentException("Assessment cannot be null");
        }

        Instant now = Instant.now();
        AssessmentStatus status = assessment.getStatus();

        return switch (status) {
            case PENDING -> computePendingProgress(assessment, now);
            case PROCESSING -> computeProcessingProgress(assessment, now);
            case COMPLETED -> computeCompletedProgress(assessment, now);
            case FAILED -> computeFailedProgress(assessment, now);
        };
    }

    /**
     * Compute progress for PENDING status (in queue).
     */
    private AssessmentProgressEvent computePendingProgress(Assessment assessment, Instant now) {
        // MVP: Mock queue position (random 1-10)
        Integer queuePosition = (int) ((System.nanoTime() % 10) + 1);

        return new AssessmentProgressEvent(
            assessment.getId(),
            AssessmentStatus.PENDING,
            0,
            STEP_PENDING,
            (int) ESTIMATED_TOTAL_DURATION_SECONDS,
            queuePosition,
            0.0,
            now
        );
    }

    /**
     * Compute progress for PROCESSING status (analysis in progress).
     */
    private AssessmentProgressEvent computeProcessingProgress(Assessment assessment, Instant now) {
        Instant startedAt = assessment.getStartedAt();
        if (startedAt == null) {
            startedAt = assessment.getCreatedAt();
        }

        long elapsedSeconds = Duration.between(startedAt, now).getSeconds();
        elapsedSeconds = Math.max(0, elapsedSeconds);

        // Calculate progress percentage (0-99 while PROCESSING)
        int progressPercent = (int) Math.min(99, (elapsedSeconds * 100) / ESTIMATED_TOTAL_DURATION_SECONDS);

        // Estimate remaining time
        long remainingSeconds = Math.max(1, ESTIMATED_TOTAL_DURATION_SECONDS - elapsedSeconds);

        // Estimate confidence based on progress (higher progress = higher confidence, up to 0.85)
        double confidence = Math.min(0.85, 0.1 + (progressPercent * 0.75) / 100.0);

        return new AssessmentProgressEvent(
            assessment.getId(),
            AssessmentStatus.PROCESSING,
            progressPercent,
            STEP_PROCESSING,
            (int) remainingSeconds,
            null, // No queue position when PROCESSING
            confidence,
            now
        );
    }

    /**
     * Compute progress for COMPLETED status.
     */
    private AssessmentProgressEvent computeCompletedProgress(Assessment assessment, Instant now) {
        return new AssessmentProgressEvent(
            assessment.getId(),
            AssessmentStatus.COMPLETED,
            100,
            STEP_COMPLETED,
            0,
            null,
            0.95, // High confidence for completed assessment
            now
        );
    }

    /**
     * Compute progress for FAILED status.
     */
    private AssessmentProgressEvent computeFailedProgress(Assessment assessment, Instant now) {
        String errorStep = STEP_FAILED;
        if (assessment.getErrorMessage() != null && !assessment.getErrorMessage().isEmpty()) {
            errorStep = "Error: " + assessment.getErrorMessage();
        }

        return new AssessmentProgressEvent(
            assessment.getId(),
            AssessmentStatus.FAILED,
            0,
            errorStep,
            0,
            null,
            0.0, // Zero confidence for failed assessment
            now
        );
    }
}
