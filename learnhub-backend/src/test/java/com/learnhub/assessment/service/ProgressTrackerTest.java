package com.learnhub.assessment.service;

import com.learnhub.assessment.dto.AssessmentProgressEvent;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ProgressTracker service.
 * Tests progress computation for each assessment status.
 */
@DisplayName("ProgressTracker Service Tests")
class ProgressTrackerTest {

    private ProgressTracker progressTracker;

    @BeforeEach
    void setUp() {
        progressTracker = new ProgressTracker();
    }

    @Test
    @DisplayName("should compute progress for PENDING status")
    void testComputeProgressPending() {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        Assessment assessment = Assessment.builder()
            .id(assessmentId)
            .userId(UUID.randomUUID())
            .snapshotId(UUID.randomUUID())
            .status(AssessmentStatus.PENDING)
            .createdAt(Instant.now())
            .build();

        // Act
        AssessmentProgressEvent event = progressTracker.computeProgress(assessment);

        // Assert
        assertThat(event).isNotNull();
        assertThat(event.assessmentId()).isEqualTo(assessmentId);
        assertThat(event.status()).isEqualTo(AssessmentStatus.PENDING);
        assertThat(event.progressPercent()).isZero();
        assertThat(event.currentStep()).contains("queue"); // Case insensitive
        assertThat(event.queuePosition()).isNotNull(); // Queue position when PENDING
        assertThat(event.confidence()).isZero();
        assertThat(event.timestamp()).isNotNull();
        assertThat(event.estimatedSecondsRemaining()).isNotNull();
    }

    @Test
    @DisplayName("should compute progress for PROCESSING status")
    void testComputeProgressProcessing() {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        Assessment assessment = Assessment.builder()
            .id(assessmentId)
            .userId(UUID.randomUUID())
            .snapshotId(UUID.randomUUID())
            .status(AssessmentStatus.PROCESSING)
            .createdAt(Instant.now().minusSeconds(30))
            .startedAt(Instant.now().minusSeconds(15))
            .build();

        // Act
        AssessmentProgressEvent event = progressTracker.computeProgress(assessment);

        // Assert
        assertThat(event).isNotNull();
        assertThat(event.assessmentId()).isEqualTo(assessmentId);
        assertThat(event.status()).isEqualTo(AssessmentStatus.PROCESSING);
        assertThat(event.progressPercent()).isBetween(1, 99);
        assertThat(event.currentStep()).isNotEmpty();
        assertThat(event.queuePosition()).isNull(); // No queue position when PROCESSING
        assertThat(event.confidence()).isBetween(0.0, 1.0);
        assertThat(event.timestamp()).isNotNull();
        assertThat(event.estimatedSecondsRemaining()).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("should compute progress for COMPLETED status")
    void testComputeProgressCompleted() {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        Assessment assessment = Assessment.builder()
            .id(assessmentId)
            .userId(UUID.randomUUID())
            .snapshotId(UUID.randomUUID())
            .status(AssessmentStatus.COMPLETED)
            .createdAt(Instant.now().minusSeconds(60))
            .startedAt(Instant.now().minusSeconds(45))
            .completedAt(Instant.now())
            .build();

        // Act
        AssessmentProgressEvent event = progressTracker.computeProgress(assessment);

        // Assert
        assertThat(event).isNotNull();
        assertThat(event.assessmentId()).isEqualTo(assessmentId);
        assertThat(event.status()).isEqualTo(AssessmentStatus.COMPLETED);
        assertThat(event.progressPercent()).isEqualTo(100);
        assertThat(event.currentStep()).contains("Complete", "complete", "completed");
        assertThat(event.queuePosition()).isNull();
        assertThat(event.confidence()).isBetween(0.0, 1.0);
        assertThat(event.timestamp()).isNotNull();
        assertThat(event.estimatedSecondsRemaining()).isZero();
    }

    @Test
    @DisplayName("should compute progress for FAILED status")
    void testComputeProgressFailed() {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        Assessment assessment = Assessment.builder()
            .id(assessmentId)
            .userId(UUID.randomUUID())
            .snapshotId(UUID.randomUUID())
            .status(AssessmentStatus.FAILED)
            .createdAt(Instant.now().minusSeconds(60))
            .startedAt(Instant.now().minusSeconds(45))
            .completedAt(Instant.now())
            .errorMessage("Analysis engine timeout")
            .build();

        // Act
        AssessmentProgressEvent event = progressTracker.computeProgress(assessment);

        // Assert
        assertThat(event).isNotNull();
        assertThat(event.assessmentId()).isEqualTo(assessmentId);
        assertThat(event.status()).isEqualTo(AssessmentStatus.FAILED);
        assertThat(event.progressPercent()).isZero();
        assertThat(event.currentStep()).contains("Failed", "failed", "Error", "error");
        assertThat(event.queuePosition()).isNull();
        assertThat(event.confidence()).isZero();
        assertThat(event.timestamp()).isNotNull();
        assertThat(event.estimatedSecondsRemaining()).isZero();
    }

    @Test
    @DisplayName("should throw exception if assessment is null")
    void testComputeProgressNullAssessment() {
        // Act & Assert
        assertThatThrownBy(() -> progressTracker.computeProgress(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Assessment");
    }

    @Test
    @DisplayName("should calculate progress percentage based on elapsed time during PROCESSING")
    void testProgressPercentageIncreasesOverTime() {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        // Assessment started 10 seconds ago
        Assessment earlyAssessment = Assessment.builder()
            .id(assessmentId)
            .userId(userId)
            .snapshotId(snapshotId)
            .status(AssessmentStatus.PROCESSING)
            .createdAt(Instant.now().minusSeconds(60))
            .startedAt(Instant.now().minusSeconds(10))
            .build();

        // Assessment started 40 seconds ago (more progress)
        Assessment lateAssessment = Assessment.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .snapshotId(snapshotId)
            .status(AssessmentStatus.PROCESSING)
            .createdAt(Instant.now().minusSeconds(60))
            .startedAt(Instant.now().minusSeconds(40))
            .build();

        // Act
        AssessmentProgressEvent earlyEvent = progressTracker.computeProgress(earlyAssessment);
        AssessmentProgressEvent lateEvent = progressTracker.computeProgress(lateAssessment);

        // Assert
        assertThat(lateEvent.progressPercent()).isGreaterThan(earlyEvent.progressPercent());
    }

    @Test
    @DisplayName("should estimate time remaining based on progress during PROCESSING")
    void testEstimatedTimeDecreases() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        Assessment earlyAssessment = Assessment.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .snapshotId(snapshotId)
            .status(AssessmentStatus.PROCESSING)
            .createdAt(Instant.now().minusSeconds(60))
            .startedAt(Instant.now().minusSeconds(10))
            .build();

        Assessment lateAssessment = Assessment.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .snapshotId(snapshotId)
            .status(AssessmentStatus.PROCESSING)
            .createdAt(Instant.now().minusSeconds(60))
            .startedAt(Instant.now().minusSeconds(40))
            .build();

        // Act
        AssessmentProgressEvent earlyEvent = progressTracker.computeProgress(earlyAssessment);
        AssessmentProgressEvent lateEvent = progressTracker.computeProgress(lateAssessment);

        // Assert
        assertThat(earlyEvent.estimatedSecondsRemaining())
            .isGreaterThan(lateEvent.estimatedSecondsRemaining());
    }

    @Test
    @DisplayName("should have consistent timestamp across identical calls")
    void testTimestampConsistency() {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        Assessment assessment = Assessment.builder()
            .id(assessmentId)
            .userId(UUID.randomUUID())
            .snapshotId(UUID.randomUUID())
            .status(AssessmentStatus.PENDING)
            .createdAt(Instant.now())
            .build();

        // Act
        AssessmentProgressEvent event1 = progressTracker.computeProgress(assessment);
        AssessmentProgressEvent event2 = progressTracker.computeProgress(assessment);

        // Assert - timestamps should be very close (within 1 second)
        long diffSeconds = Math.abs(event1.timestamp().getEpochSecond() - event2.timestamp().getEpochSecond());
        assertThat(diffSeconds).isLessThanOrEqualTo(1L);
    }
}
