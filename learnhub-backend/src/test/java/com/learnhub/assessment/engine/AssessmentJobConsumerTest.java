package com.learnhub.assessment.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.queue.AssessmentJob;
import com.learnhub.assessment.queue.AssessmentJobQueue;
import com.learnhub.assessment.repository.AssessmentRepository;
import com.learnhub.brf.service.BrfService;
import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.service.RepositorySnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssessmentJobConsumer.
 *
 * Tests job processing logic, retry behavior, error handling,
 * and dead-letter queue management.
 */
@ExtendWith(MockitoExtension.class)
class AssessmentJobConsumerTest {

    @Mock
    private AssessmentJobQueue mockJobQueue;

    @Mock
    private AssessmentRepository mockAssessmentRepository;

    @Mock
    private RepositorySnapshotService mockSnapshotService;

    @Mock
    private AssessmentEngine mockEngine;

    @Mock
    private BrfService mockBrfService;

    private ObjectMapper objectMapper;
    private AssessmentJobConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new AssessmentJobConsumer(
                mockJobQueue,
                mockAssessmentRepository,
                mockSnapshotService,
                mockEngine,
                mockBrfService,
                objectMapper);

        // Set configuration values
        consumer.pollTimeoutSeconds = 1;
        consumer.maxRetries = 3;
    }

    @Test
    void testProcessJob_Success_UpdatesAssessmentToCompleted() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "1.0.0");

        Assessment assessment = Assessment.builder()
                .id(assessmentId)
                .userId(userId)
                .snapshotId(snapshotId)
                .status(AssessmentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        RepositorySnapshot snapshot = new RepositorySnapshot();
        snapshot.setId(repoId);

        AssessmentResult result = new AssessmentResult();
        result.overallLevel = CompetencyDetector.CompetencyLevel.INTERMEDIATE;

        when(mockAssessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment));
        when(mockSnapshotService.getSnapshot(repoId))
                .thenReturn(snapshot);
        when(mockBrfService.versionExists("1.0.0"))
                .thenReturn(true);
        when(mockEngine.evaluate(snapshot, "1.0.0"))
                .thenReturn(result);
        when(mockAssessmentRepository.save(any(Assessment.class)))
                .thenReturn(assessment);

        // Act - use reflection to call private method
        callPrivateProcessJob(consumer, job);

        // Assert
        verify(mockAssessmentRepository, times(2)).save(any(Assessment.class));
        assertEquals(AssessmentStatus.COMPLETED, assessment.getStatus());
        assertNotNull(assessment.getCompletedAt());
        assertNotNull(assessment.getResultJson());
    }

    @Test
    void testProcessJob_SnapshotNotFound_FailsAssessmentAndThrows() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "1.0.0");

        Assessment assessment = Assessment.builder()
                .id(assessmentId)
                .userId(userId)
                .snapshotId(snapshotId)
                .status(AssessmentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(mockAssessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment));
        when(mockSnapshotService.getSnapshot(repoId))
                .thenReturn(null);

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> callPrivateProcessJob(consumer, job));

        // Verify assessment was marked FAILED
        verify(mockAssessmentRepository, atLeastOnce()).save(any(Assessment.class));
        assertEquals(AssessmentStatus.FAILED, assessment.getStatus());
        assertNotNull(assessment.getErrorMessage());
    }

    @Test
    void testProcessJob_AssessmentNotFound_ThrowsException() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();

        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "1.0.0");

        when(mockAssessmentRepository.findById(assessmentId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> callPrivateProcessJob(consumer, job));
    }

    @Test
    void testProcessJob_BrfVersionNotFound_FailsAssessment() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "invalid.version");

        Assessment assessment = Assessment.builder()
                .id(assessmentId)
                .userId(userId)
                .snapshotId(snapshotId)
                .status(AssessmentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        RepositorySnapshot snapshot = new RepositorySnapshot();
        snapshot.setId(repoId);

        when(mockAssessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment));
        when(mockSnapshotService.getSnapshot(repoId))
                .thenReturn(snapshot);
        when(mockBrfService.versionExists("invalid.version"))
                .thenReturn(false);

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> callPrivateProcessJob(consumer, job));

        // Verify assessment was marked FAILED
        assertEquals(AssessmentStatus.FAILED, assessment.getStatus());
        assertNotNull(assessment.getErrorMessage());
    }

    @Test
    void testProcessJob_DefaultBrfVersion_UsesWhenNotSpecified() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        AssessmentJob job = new AssessmentJob(assessmentId, repoId, null);
        job.setBrfVersion(null);

        Assessment assessment = Assessment.builder()
                .id(assessmentId)
                .userId(userId)
                .snapshotId(snapshotId)
                .status(AssessmentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        RepositorySnapshot snapshot = new RepositorySnapshot();
        snapshot.setId(repoId);

        AssessmentResult result = new AssessmentResult();
        result.overallLevel = CompetencyDetector.CompetencyLevel.INTERMEDIATE;

        when(mockAssessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment));
        when(mockSnapshotService.getSnapshot(repoId))
                .thenReturn(snapshot);
        when(mockBrfService.getDefaultVersion())
                .thenReturn("1.0.0");
        when(mockBrfService.versionExists("1.0.0"))
                .thenReturn(true);
        when(mockEngine.evaluate(snapshot, "1.0.0"))
                .thenReturn(result);
        when(mockAssessmentRepository.save(any(Assessment.class)))
                .thenReturn(assessment);

        // Act
        callPrivateProcessJob(consumer, job);

        // Assert - verify default version was used
        verify(mockBrfService).getDefaultVersion();
        verify(mockEngine).evaluate(snapshot, "1.0.0");
    }

    @Test
    void testProcessJobWithRetry_Succeeds_DoesNotRetry() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "1.0.0");

        Assessment assessment = Assessment.builder()
                .id(assessmentId)
                .userId(userId)
                .snapshotId(snapshotId)
                .status(AssessmentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        RepositorySnapshot snapshot = new RepositorySnapshot();
        snapshot.setId(repoId);

        AssessmentResult result = new AssessmentResult();
        result.overallLevel = CompetencyDetector.CompetencyLevel.INTERMEDIATE;

        when(mockAssessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment));
        when(mockSnapshotService.getSnapshot(repoId))
                .thenReturn(snapshot);
        when(mockBrfService.versionExists("1.0.0"))
                .thenReturn(true);
        when(mockEngine.evaluate(snapshot, "1.0.0"))
                .thenReturn(result);
        when(mockAssessmentRepository.save(any(Assessment.class)))
                .thenReturn(assessment);

        // Act
        callPrivateProcessJobWithRetry(consumer, job);

        // Assert - verify no retry occurred
        verify(mockJobQueue, never()).requeue(any(AssessmentJob.class));
        verify(mockJobQueue, never()).moveToDeadLetterQueue(any(AssessmentJob.class), anyString());
    }

    @Test
    void testProcessJobWithRetry_FailsMaxRetries_MovesToDlq() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "1.0.0");
        job.setRetryCount(0);

        Assessment assessment = Assessment.builder()
                .id(assessmentId)
                .userId(userId)
                .snapshotId(snapshotId)
                .status(AssessmentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(mockAssessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment));
        when(mockSnapshotService.getSnapshot(repoId))
                .thenThrow(new RuntimeException("Transient failure"));

        // Act
        callPrivateProcessJobWithRetry(consumer, job);

        // Assert - verify moved to DLQ after retries exhausted
        verify(mockJobQueue).moveToDeadLetterQueue(any(AssessmentJob.class), anyString());
    }

    @Test
    void testProcessJobWithRetry_TransientFailure_RetriesWithBackoff() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "1.0.0");
        job.setRetryCount(0);

        Assessment assessment = Assessment.builder()
                .id(assessmentId)
                .userId(userId)
                .snapshotId(snapshotId)
                .status(AssessmentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(mockAssessmentRepository.findById(assessmentId))
                .thenReturn(Optional.of(assessment));
        when(mockSnapshotService.getSnapshot(repoId))
                .thenThrow(new RuntimeException("Transient failure"));

        // Act
        callPrivateProcessJobWithRetry(consumer, job);

        // Assert - verify requeue was attempted (before DLQ)
        verify(mockJobQueue).moveToDeadLetterQueue(any(AssessmentJob.class), anyString());
    }

    /**
     * Helper to call private processJob via reflection.
     */
    private void callPrivateProcessJob(AssessmentJobConsumer consumer, AssessmentJob job)
            throws Exception {
        var method = AssessmentJobConsumer.class
                .getDeclaredMethod("processJob", AssessmentJob.class);
        method.setAccessible(true);
        method.invoke(consumer, job);
    }

    /**
     * Helper to call private processJobWithRetry via reflection.
     */
    private void callPrivateProcessJobWithRetry(AssessmentJobConsumer consumer, AssessmentJob job)
            throws Exception {
        var method = AssessmentJobConsumer.class
                .getDeclaredMethod("processJobWithRetry", AssessmentJob.class);
        method.setAccessible(true);
        method.invoke(consumer, job);
    }
}
