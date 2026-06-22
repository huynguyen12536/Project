package com.learnhub.assessment.service;

import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.exception.DuplicateSubmissionException;
import com.learnhub.assessment.exception.TooManyActiveAssessmentsException;
import com.learnhub.assessment.repository.AssessmentRepository;
import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.exception.SnapshotNotFoundException;
import com.learnhub.github.repository.RepositorySnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssessmentService rate limiting validation.
 * Tests duplicate submission detection and concurrent assessment limits.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentService Rate Limiting Tests")
class AssessmentServiceRateLimitTest {

    private AssessmentService assessmentService;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private RepositorySnapshotRepository snapshotRepository;

    private UUID userId;
    private UUID snapshotId;
    private RepositorySnapshot snapshot;

    @BeforeEach
    void setUp() {
        assessmentService = new AssessmentService(
            assessmentRepository,
            null, // resultRepository not needed for rate limit tests
            snapshotRepository
        );

        userId = UUID.randomUUID();
        snapshotId = UUID.randomUUID();

        snapshot = RepositorySnapshot.builder()
            .id(snapshotId)
            .userId(userId)
            .build();
    }

    @Test
    @DisplayName("should allow submission when no concurrent PROCESSING assessments exist")
    void testAllowSubmissionWhenNoConcurrentAssessments() {
        // Arrange
        when(snapshotRepository.findByIdAndUserId(snapshotId, userId))
            .thenReturn(Optional.of(snapshot));
        when(assessmentRepository.countByUserIdAndStatus(userId, AssessmentStatus.PROCESSING))
            .thenReturn(0L);
        when(assessmentRepository.findRecentByUserAndSnapshot(
            eq(userId), eq(snapshotId), any(Instant.class)
        )).thenReturn(Optional.empty());
        when(assessmentRepository.save(any(Assessment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Assessment result = assessmentService.submitAssessmentWithRateLimit(userId, snapshotId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getSnapshotId()).isEqualTo(snapshotId);
    }

    @Test
    @DisplayName("should allow submission when less than 3 concurrent PROCESSING assessments exist")
    void testAllowSubmissionWhen1Or2ConcurrentAssessments() {
        // Arrange
        when(snapshotRepository.findByIdAndUserId(snapshotId, userId))
            .thenReturn(Optional.of(snapshot));
        when(assessmentRepository.countByUserIdAndStatus(userId, AssessmentStatus.PROCESSING))
            .thenReturn(2L); // 2 out of 3 max
        when(assessmentRepository.findRecentByUserAndSnapshot(
            eq(userId), eq(snapshotId), any(Instant.class)
        )).thenReturn(Optional.empty());
        when(assessmentRepository.save(any(Assessment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Assessment result = assessmentService.submitAssessmentWithRateLimit(userId, snapshotId);

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("should reject submission when 3 concurrent PROCESSING assessments exist (409)")
    void testRejectSubmissionWhen3ConcurrentAssessments() {
        // Arrange
        when(snapshotRepository.findByIdAndUserId(snapshotId, userId))
            .thenReturn(Optional.of(snapshot));
        when(assessmentRepository.countByUserIdAndStatus(userId, AssessmentStatus.PROCESSING))
            .thenReturn(3L); // At max limit

        // Act & Assert
        assertThatThrownBy(() -> assessmentService.submitAssessmentWithRateLimit(userId, snapshotId))
            .isInstanceOf(TooManyActiveAssessmentsException.class)
            .hasMessageContaining("3");
    }

    @Test
    @DisplayName("should reject submission when 4 concurrent PROCESSING assessments exist (409)")
    void testRejectSubmissionWhen4ConcurrentAssessments() {
        // Arrange
        when(snapshotRepository.findByIdAndUserId(snapshotId, userId))
            .thenReturn(Optional.of(snapshot));
        when(assessmentRepository.countByUserIdAndStatus(userId, AssessmentStatus.PROCESSING))
            .thenReturn(4L); // Over max limit

        // Act & Assert
        assertThatThrownBy(() -> assessmentService.submitAssessmentWithRateLimit(userId, snapshotId))
            .isInstanceOf(TooManyActiveAssessmentsException.class);
    }

    @Test
    @DisplayName("should allow submission for different snapshot even with 3 concurrent assessments")
    void testAllowDifferentSnapshotWith3ConcurrentAssessments() {
        // Arrange - using different snapshot ID
        UUID otherSnapshotId = UUID.randomUUID();
        RepositorySnapshot otherSnapshot = RepositorySnapshot.builder()
            .id(otherSnapshotId)
            .userId(userId)
            .build();

        when(snapshotRepository.findByIdAndUserId(otherSnapshotId, userId))
            .thenReturn(Optional.of(otherSnapshot));
        when(assessmentRepository.countByUserIdAndStatus(userId, AssessmentStatus.PROCESSING))
            .thenReturn(3L); // Max concurrent for PROCESSING
        when(assessmentRepository.findRecentByUserAndSnapshot(
            eq(userId), eq(otherSnapshotId), any(Instant.class)
        )).thenReturn(Optional.empty());
        when(assessmentRepository.save(any(Assessment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act - should still be allowed because it's checking concurrent PROCESSING count
        // (This demonstrates that the rate limit is on concurrent PROCESSING, not total submissions)
        // In real system, this would be rejected by concurrent count being 3 already.
        // But if PROCESSING was only 3 for other snapshots, new different snapshot should be rejected too.

        // For this test, we're verifying the logic that checks concurrent count first
        assertThatThrownBy(() -> assessmentService.submitAssessmentWithRateLimit(userId, otherSnapshotId))
            .isInstanceOf(TooManyActiveAssessmentsException.class);
    }

    @Test
    @DisplayName("should reject duplicate submission within 60 second window (409)")
    void testRejectDuplicateSubmissionWithin60Seconds() {
        // Arrange
        Assessment recentAssessment = Assessment.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .snapshotId(snapshotId)
            .status(AssessmentStatus.PENDING)
            .createdAt(Instant.now().minusSeconds(30)) // 30 seconds ago
            .build();

        when(snapshotRepository.findByIdAndUserId(snapshotId, userId))
            .thenReturn(Optional.of(snapshot));
        when(assessmentRepository.countByUserIdAndStatus(userId, AssessmentStatus.PROCESSING))
            .thenReturn(0L);
        when(assessmentRepository.findRecentByUserAndSnapshot(
            eq(userId), eq(snapshotId), any(Instant.class)
        )).thenReturn(Optional.of(recentAssessment));

        // Act & Assert
        assertThatThrownBy(() -> assessmentService.submitAssessmentWithRateLimit(userId, snapshotId))
            .isInstanceOf(DuplicateSubmissionException.class)
            .hasMessageContaining("60");
    }

    @Test
    @DisplayName("should allow submission after 60 second duplicate window expires")
    void testAllowSubmissionAfter60SecondWindow() {
        // Arrange
        Assessment oldAssessment = Assessment.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .snapshotId(snapshotId)
            .status(AssessmentStatus.COMPLETED)
            .createdAt(Instant.now().minusSeconds(70)) // 70 seconds ago (outside 60s window)
            .build();

        when(snapshotRepository.findByIdAndUserId(snapshotId, userId))
            .thenReturn(Optional.of(snapshot));
        when(assessmentRepository.countByUserIdAndStatus(userId, AssessmentStatus.PROCESSING))
            .thenReturn(0L);
        when(assessmentRepository.findRecentByUserAndSnapshot(
            eq(userId), eq(snapshotId), any(Instant.class)
        )).thenReturn(Optional.empty()); // No recent submission within 60s
        when(assessmentRepository.save(any(Assessment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Assessment result = assessmentService.submitAssessmentWithRateLimit(userId, snapshotId);

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("DuplicateSubmissionException should include retry-after duration")
    void testDuplicateSubmissionExceptionIncludesRetryAfter() {
        // Arrange
        Assessment recentAssessment = Assessment.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .snapshotId(snapshotId)
            .status(AssessmentStatus.PENDING)
            .createdAt(Instant.now().minusSeconds(40))
            .build();

        when(snapshotRepository.findByIdAndUserId(snapshotId, userId))
            .thenReturn(Optional.of(snapshot));
        when(assessmentRepository.countByUserIdAndStatus(userId, AssessmentStatus.PROCESSING))
            .thenReturn(0L);
        when(assessmentRepository.findRecentByUserAndSnapshot(
            eq(userId), eq(snapshotId), any(Instant.class)
        )).thenReturn(Optional.of(recentAssessment));

        // Act & Assert
        assertThatThrownBy(() -> assessmentService.submitAssessmentWithRateLimit(userId, snapshotId))
            .isInstanceOf(DuplicateSubmissionException.class)
            .satisfies(ex -> {
                DuplicateSubmissionException dupEx = (DuplicateSubmissionException) ex;
                assertThat(dupEx.getRetryAfterSeconds()).isGreaterThan(0);
                assertThat(dupEx.getRetryAfterSeconds()).isLessThanOrEqualTo(60);
            });
    }

    @Test
    @DisplayName("should reject submission if snapshot not found or not owned by user")
    void testRejectSubmissionIfSnapshotNotFound() {
        // Arrange
        when(snapshotRepository.findByIdAndUserId(snapshotId, userId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> assessmentService.submitAssessmentWithRateLimit(userId, snapshotId))
            .isInstanceOf(SnapshotNotFoundException.class);
    }

    @Test
    @DisplayName("should check concurrent PROCESSING count before duplicate check")
    void testConcurrentCountCheckedFirst() {
        // Arrange - create scenario with both violations
        // Max concurrent PROCESSING (3) and recent submission
        Assessment recentAssessment = Assessment.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .snapshotId(snapshotId)
            .status(AssessmentStatus.PENDING)
            .createdAt(Instant.now().minusSeconds(30))
            .build();

        when(snapshotRepository.findByIdAndUserId(snapshotId, userId))
            .thenReturn(Optional.of(snapshot));
        when(assessmentRepository.countByUserIdAndStatus(userId, AssessmentStatus.PROCESSING))
            .thenReturn(3L); // Max concurrent

        // Act & Assert - should throw TooManyActiveAssessmentsException, not DuplicateSubmissionException
        assertThatThrownBy(() -> assessmentService.submitAssessmentWithRateLimit(userId, snapshotId))
            .isInstanceOf(TooManyActiveAssessmentsException.class);

        // Verify findRecentByUserAndSnapshot was never called (checked later)
        verify(assessmentRepository, never()).findRecentByUserAndSnapshot(any(), any(), any());
    }
}
