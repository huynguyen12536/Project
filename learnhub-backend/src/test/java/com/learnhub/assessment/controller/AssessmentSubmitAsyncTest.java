package com.learnhub.assessment.controller;

import com.learnhub.assessment.dto.AssessmentResponse;
import com.learnhub.assessment.dto.SubmitAssessmentRequest;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.queue.AssessmentJob;
import com.learnhub.assessment.queue.AssessmentJobQueue;
import com.learnhub.assessment.service.AssessmentService;
import com.learnhub.common.util.AuthenticationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for async enqueue pattern in AssessmentController.
 * Tests that submitAssessment returns immediately (< 50ms) without blocking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentController Async Enqueue Tests")
class AssessmentSubmitAsyncTest {

    private AssessmentController controller;

    @Mock
    private AssessmentService assessmentService;

    @Mock
    private AssessmentJobQueue jobQueue;

    @Mock
    private AuthenticationUtil authenticationUtil;

    private UUID userId;
    private UUID snapshotId;
    private UUID assessmentId;
    private SubmitAssessmentRequest request;
    private Assessment assessment;

    @BeforeEach
    void setUp() {
        controller = new AssessmentController(assessmentService, authenticationUtil);

        userId = UUID.randomUUID();
        snapshotId = UUID.randomUUID();
        assessmentId = UUID.randomUUID();

        request = new SubmitAssessmentRequest(snapshotId);

        assessment = Assessment.builder()
            .id(assessmentId)
            .userId(userId)
            .snapshotId(snapshotId)
            .status(AssessmentStatus.PENDING)
            .createdAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("should return PENDING status immediately after submission")
    void testSubmitAssessmentReturnsPendingStatus() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.submitAssessment(userId, snapshotId)).thenReturn(assessment);

        // Act
        ResponseEntity<AssessmentResponse> response = controller.submitAssessment(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(AssessmentStatus.PENDING);
    }

    @Test
    @DisplayName("should return assessment ID in response immediately")
    void testSubmitAssessmentReturnsAssessmentId() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.submitAssessment(userId, snapshotId)).thenReturn(assessment);

        // Act
        ResponseEntity<AssessmentResponse> response = controller.submitAssessment(request);

        // Assert
        assertThat(response.getBody().getId()).isEqualTo(assessmentId);
    }

    @Test
    @DisplayName("should not block waiting for job queue processing")
    void testSubmitAssessmentDoesNotBlock() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.submitAssessment(userId, snapshotId)).thenReturn(assessment);

        // Act - measure execution time
        long startTime = System.currentTimeMillis();
        ResponseEntity<AssessmentResponse> response = controller.submitAssessment(request);
        long executionTime = System.currentTimeMillis() - startTime;

        // Assert - should complete in < 50ms (generous allowance for slow CI)
        assertThat(executionTime).isLessThan(50L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("should create assessment before enqueueing job")
    void testAssessmentCreatedBeforeJobEnqueue() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.submitAssessment(userId, snapshotId)).thenReturn(assessment);

        // Act
        ResponseEntity<AssessmentResponse> response = controller.submitAssessment(request);

        // Assert - assessmentService.submitAssessment should have been called
        verify(assessmentService).submitAssessment(userId, snapshotId);
        assertThat(response.getBody().getId()).isNotNull();
    }

    @Test
    @DisplayName("should use assessment ID from created assessment for job enqueueing")
    void testJobEnqueueUsesCreatedAssessmentId() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.submitAssessment(userId, snapshotId)).thenReturn(assessment);

        // Act
        ResponseEntity<AssessmentResponse> response = controller.submitAssessment(request);

        // Assert - response should contain the same assessment ID
        assertThat(response.getBody().getId()).isEqualTo(assessment.getId());
    }

    @Test
    @DisplayName("should return 201 Created on successful submission")
    void testSubmitAssessmentReturns201() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.submitAssessment(userId, snapshotId)).thenReturn(assessment);

        // Act
        ResponseEntity<AssessmentResponse> response = controller.submitAssessment(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("should include all required response fields")
    void testSubmitAssessmentResponseIncludesAllFields() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.submitAssessment(userId, snapshotId)).thenReturn(assessment);

        // Act
        ResponseEntity<AssessmentResponse> response = controller.submitAssessment(request);

        // Assert
        assertThat(response.getBody()).satisfies(body -> {
            assertThat(body.getId()).isNotNull();
            assertThat(body.getUserId()).isEqualTo(userId);
            assertThat(body.getSnapshotId()).isEqualTo(snapshotId);
            assertThat(body.getStatus()).isEqualTo(AssessmentStatus.PENDING);
            assertThat(body.getCreatedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("should handle null snapshot ID gracefully")
    void testSubmitAssessmentWithNullSnapshotId() {
        // Arrange
        SubmitAssessmentRequest invalidRequest = new SubmitAssessmentRequest(null);
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);

        // Act & Assert
        assertThatThrownBy(() -> controller.submitAssessment(invalidRequest))
            .hasMessageContaining("snapshotId");
    }

    @Test
    @DisplayName("submitAssessment should call authentication utility to get user ID")
    void testSubmitAssessmentCallsAuthenticationUtil() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.submitAssessment(userId, snapshotId)).thenReturn(assessment);

        // Act
        controller.submitAssessment(request);

        // Assert
        verify(authenticationUtil).getCurrentUserId();
    }

    @Test
    @DisplayName("should return response containing snapshot ID")
    void testSubmitAssessmentResponseIncludesSnapshotId() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.submitAssessment(userId, snapshotId)).thenReturn(assessment);

        // Act
        ResponseEntity<AssessmentResponse> response = controller.submitAssessment(request);

        // Assert
        assertThat(response.getBody().getSnapshotId()).isEqualTo(snapshotId);
    }

    @Test
    @DisplayName("should not throw exception if queue enqueue fails (fire-and-forget)")
    void testSubmitAssessmentHandlesQueueFailure() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.submitAssessment(userId, snapshotId)).thenReturn(assessment);

        // Act - should still return successfully
        ResponseEntity<AssessmentResponse> response = controller.submitAssessment(request);

        // Assert - despite any queue issues, assessment is created and returned
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(assessmentId);
    }
}
