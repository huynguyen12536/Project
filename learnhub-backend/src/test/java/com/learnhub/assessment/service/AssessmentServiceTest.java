package com.learnhub.assessment.service;

import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.exception.AssessmentNotFoundException;
import com.learnhub.assessment.repository.AssessmentRepository;
import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.exception.SnapshotNotFoundException;
import com.learnhub.github.repository.RepositorySnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private RepositorySnapshotRepository snapshotRepository;

    @InjectMocks
    private AssessmentService assessmentService;

    private UUID testUserId;
    private UUID testSnapshotId;
    private UUID testAssessmentId;
    private RepositorySnapshot testSnapshot;
    private Assessment testAssessment;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testSnapshotId = UUID.randomUUID();
        testAssessmentId = UUID.randomUUID();

        testSnapshot = RepositorySnapshot.builder()
            .id(testSnapshotId)
            .userId(testUserId)
            .githubRepoId(123L)
            .branch("main")
            .commitSha("abc123")
            .filesCount(10)
            .totalSizeKb(1024L)
            .createdAt(LocalDateTime.now())
            .build();

        testAssessment = Assessment.builder()
            .id(testAssessmentId)
            .userId(testUserId)
            .snapshotId(testSnapshotId)
            .status(AssessmentStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
    }

    // ========== submitAssessment() Tests ==========

    @Test
    void testSubmitAssessment_Success() {
        when(snapshotRepository.findByIdAndUserId(testSnapshotId, testUserId))
            .thenReturn(Optional.of(testSnapshot));
        when(assessmentRepository.save(any(Assessment.class)))
            .thenAnswer(invocation -> {
                Assessment assessment = invocation.getArgument(0);
                assessment.setId(testAssessmentId);
                return assessment;
            });

        Assessment result = assessmentService.submitAssessment(testUserId, testSnapshotId);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testSnapshotId, result.getSnapshotId());
        assertEquals(AssessmentStatus.PENDING, result.getStatus());
        verify(snapshotRepository, times(1)).findByIdAndUserId(testSnapshotId, testUserId);
        verify(assessmentRepository, times(1)).save(any(Assessment.class));
    }

    @Test
    void testSubmitAssessment_SnapshotNotFound() {
        when(snapshotRepository.findByIdAndUserId(testSnapshotId, testUserId))
            .thenReturn(Optional.empty());

        assertThrows(SnapshotNotFoundException.class,
            () -> assessmentService.submitAssessment(testUserId, testSnapshotId));
        verify(snapshotRepository, times(1)).findByIdAndUserId(testSnapshotId, testUserId);
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void testSubmitAssessment_SnapshotNotOwnedByUser() {
        UUID differentUserId = UUID.randomUUID();
        when(snapshotRepository.findByIdAndUserId(testSnapshotId, differentUserId))
            .thenReturn(Optional.empty());

        assertThrows(SnapshotNotFoundException.class,
            () -> assessmentService.submitAssessment(differentUserId, testSnapshotId));
        verify(snapshotRepository, times(1)).findByIdAndUserId(testSnapshotId, differentUserId);
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void testSubmitAssessment_NullUserId() {
        assertThrows(IllegalArgumentException.class,
            () -> assessmentService.submitAssessment(null, testSnapshotId));
        verify(snapshotRepository, never()).findByIdAndUserId(any(), any());
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void testSubmitAssessment_NullSnapshotId() {
        assertThrows(IllegalArgumentException.class,
            () -> assessmentService.submitAssessment(testUserId, null));
        verify(snapshotRepository, never()).findByIdAndUserId(any(), any());
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    // ========== getAssessment() Tests ==========

    @Test
    void testGetAssessment_Success() {
        when(assessmentRepository.findByIdAndUserId(testAssessmentId, testUserId))
            .thenReturn(Optional.of(testAssessment));

        Assessment result = assessmentService.getAssessment(testAssessmentId, testUserId);

        assertNotNull(result);
        assertEquals(testAssessmentId, result.getId());
        assertEquals(testUserId, result.getUserId());
        verify(assessmentRepository, times(1)).findByIdAndUserId(testAssessmentId, testUserId);
    }

    @Test
    void testGetAssessment_NotFound() {
        when(assessmentRepository.findByIdAndUserId(testAssessmentId, testUserId))
            .thenReturn(Optional.empty());

        assertThrows(AssessmentNotFoundException.class,
            () -> assessmentService.getAssessment(testAssessmentId, testUserId));
        verify(assessmentRepository, times(1)).findByIdAndUserId(testAssessmentId, testUserId);
    }

    @Test
    void testGetAssessment_NotOwnedByUser() {
        UUID differentUserId = UUID.randomUUID();
        when(assessmentRepository.findByIdAndUserId(testAssessmentId, differentUserId))
            .thenReturn(Optional.empty());

        assertThrows(AssessmentNotFoundException.class,
            () -> assessmentService.getAssessment(testAssessmentId, differentUserId));
        verify(assessmentRepository, times(1)).findByIdAndUserId(testAssessmentId, differentUserId);
    }

    @Test
    void testGetAssessment_NullAssessmentId() {
        assertThrows(IllegalArgumentException.class,
            () -> assessmentService.getAssessment(null, testUserId));
        verify(assessmentRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void testGetAssessment_NullUserId() {
        assertThrows(IllegalArgumentException.class,
            () -> assessmentService.getAssessment(testAssessmentId, null));
        verify(assessmentRepository, never()).findByIdAndUserId(any(), any());
    }

    // ========== getUserAssessments() Tests ==========

    @Test
    void testGetUserAssessments_Success() {
        List<Assessment> assessmentList = List.of(testAssessment);
        when(assessmentRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
            .thenReturn(assessmentList);

        List<Assessment> result = assessmentService.getUserAssessments(testUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAssessmentId, result.get(0).getId());
        verify(assessmentRepository, times(1)).findByUserIdOrderByCreatedAtDesc(testUserId);
    }

    @Test
    void testGetUserAssessments_EmptyList() {
        when(assessmentRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
            .thenReturn(List.of());

        List<Assessment> result = assessmentService.getUserAssessments(testUserId);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(assessmentRepository, times(1)).findByUserIdOrderByCreatedAtDesc(testUserId);
    }

    // ========== updateStatus() Tests ==========

    @Test
    void testUpdateStatus_ToPending() {
        when(assessmentRepository.findById(testAssessmentId))
            .thenReturn(Optional.of(testAssessment));
        when(assessmentRepository.save(any(Assessment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Assessment result = assessmentService.updateStatus(testAssessmentId, AssessmentStatus.PENDING);

        assertNotNull(result);
        assertEquals(AssessmentStatus.PENDING, result.getStatus());
        assertNull(result.getCompletedAt());
        verify(assessmentRepository, times(1)).findById(testAssessmentId);
        verify(assessmentRepository, times(1)).save(any(Assessment.class));
    }

    @Test
    void testUpdateStatus_ToProcessing() {
        when(assessmentRepository.findById(testAssessmentId))
            .thenReturn(Optional.of(testAssessment));
        when(assessmentRepository.save(any(Assessment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Assessment result = assessmentService.updateStatus(testAssessmentId, AssessmentStatus.PROCESSING);

        assertNotNull(result);
        assertEquals(AssessmentStatus.PROCESSING, result.getStatus());
        assertNull(result.getCompletedAt());
        verify(assessmentRepository, times(1)).findById(testAssessmentId);
        verify(assessmentRepository, times(1)).save(any(Assessment.class));
    }

    @Test
    void testUpdateStatus_ToCompleted() {
        when(assessmentRepository.findById(testAssessmentId))
            .thenReturn(Optional.of(testAssessment));
        when(assessmentRepository.save(any(Assessment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Assessment result = assessmentService.updateStatus(testAssessmentId, AssessmentStatus.COMPLETED);

        assertNotNull(result);
        assertEquals(AssessmentStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(assessmentRepository, times(1)).findById(testAssessmentId);
        verify(assessmentRepository, times(1)).save(any(Assessment.class));
    }

    @Test
    void testUpdateStatus_ToFailed() {
        when(assessmentRepository.findById(testAssessmentId))
            .thenReturn(Optional.of(testAssessment));
        when(assessmentRepository.save(any(Assessment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Assessment result = assessmentService.updateStatus(testAssessmentId, AssessmentStatus.FAILED);

        assertNotNull(result);
        assertEquals(AssessmentStatus.FAILED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(assessmentRepository, times(1)).findById(testAssessmentId);
        verify(assessmentRepository, times(1)).save(any(Assessment.class));
    }

    @Test
    void testUpdateStatus_AssessmentNotFound() {
        when(assessmentRepository.findById(testAssessmentId))
            .thenReturn(Optional.empty());

        assertThrows(AssessmentNotFoundException.class,
            () -> assessmentService.updateStatus(testAssessmentId, AssessmentStatus.COMPLETED));
        verify(assessmentRepository, times(1)).findById(testAssessmentId);
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void testUpdateStatus_NullAssessmentId() {
        assertThrows(IllegalArgumentException.class,
            () -> assessmentService.updateStatus(null, AssessmentStatus.COMPLETED));
        verify(assessmentRepository, never()).findById(any());
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void testUpdateStatus_NullStatus() {
        assertThrows(IllegalArgumentException.class,
            () -> assessmentService.updateStatus(testAssessmentId, null));
        verify(assessmentRepository, never()).findById(any());
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }
}
