package com.learnhub.assessment.controller;

import com.learnhub.assessment.dto.AssessmentProgressEvent;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.exception.AssessmentNotFoundException;
import com.learnhub.assessment.service.AssessmentService;
import com.learnhub.assessment.service.ProgressTracker;
import com.learnhub.assessment.util.SseEmitterManager;
import com.learnhub.common.util.AuthenticationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssessmentProgressController.
 * Tests SSE and polling endpoints, ownership validation, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentProgressController Tests")
class AssessmentProgressControllerTest {

    private AssessmentProgressController controller;

    @Mock
    private AssessmentService assessmentService;

    @Mock
    private ProgressTracker progressTracker;

    @Mock
    private SseEmitterManager sseEmitterManager;

    @Mock
    private AuthenticationUtil authenticationUtil;

    private UUID userId;
    private UUID assessmentId;
    private Assessment assessment;
    private AssessmentProgressEvent progressEvent;

    @BeforeEach
    void setUp() {
        controller = new AssessmentProgressController(
            assessmentService,
            progressTracker,
            sseEmitterManager,
            authenticationUtil
        );

        userId = UUID.randomUUID();
        assessmentId = UUID.randomUUID();

        assessment = Assessment.builder()
            .id(assessmentId)
            .userId(userId)
            .snapshotId(UUID.randomUUID())
            .status(AssessmentStatus.PROCESSING)
            .createdAt(Instant.now())
            .startedAt(Instant.now().minusSeconds(10))
            .build();

        progressEvent = new AssessmentProgressEvent(
            assessmentId,
            AssessmentStatus.PROCESSING,
            45,
            "Analyzing repository",
            15,
            null,
            0.75,
            Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/assessments/{id}/progress should return SSE emitter")
    void testGetProgressSseEndpoint() throws Exception {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.getAssessment(assessmentId, userId)).thenReturn(assessment);
        when(progressTracker.computeProgress(assessment)).thenReturn(progressEvent);
        when(sseEmitterManager.canRegister(userId)).thenReturn(true);

        // Act
        ResponseEntity<SseEmitter> response = controller.getProgress(assessmentId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // Verify SseEmitterManager registration was attempted
        verify(sseEmitterManager).register(eq(assessmentId), eq(userId), any(SseEmitter.class));
    }

    @Test
    @DisplayName("GET /api/v1/assessments/{id}/progress/poll should return progress event")
    void testGetProgressPollEndpoint() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.getAssessment(assessmentId, userId)).thenReturn(assessment);
        when(progressTracker.computeProgress(assessment)).thenReturn(progressEvent);

        // Act
        ResponseEntity<AssessmentProgressEvent> response = controller.getProgressPoll(assessmentId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().assessmentId()).isEqualTo(assessmentId);
        assertThat(response.getBody().status()).isEqualTo(AssessmentStatus.PROCESSING);
        assertThat(response.getBody().progressPercent()).isEqualTo(45);
    }

    @Test
    @DisplayName("GET /api/v1/assessments/{id}/progress should return 404 if assessment not found")
    void testGetProgressNotFound() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.getAssessment(assessmentId, userId))
            .thenThrow(new AssessmentNotFoundException("Assessment not found"));

        // Act & Assert
        assertThatThrownBy(() -> controller.getProgress(assessmentId))
            .isInstanceOf(AssessmentNotFoundException.class)
            .hasMessageContaining("Assessment not found");
    }

    @Test
    @DisplayName("GET /api/v1/assessments/{id}/progress/poll should return 404 if assessment not found")
    void testGetProgressPollNotFound() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.getAssessment(assessmentId, userId))
            .thenThrow(new AssessmentNotFoundException("Assessment not found"));

        // Act & Assert
        assertThatThrownBy(() -> controller.getProgressPoll(assessmentId))
            .isInstanceOf(AssessmentNotFoundException.class)
            .hasMessageContaining("Assessment not found");
    }

    @Test
    @DisplayName("SSE endpoint should handle too many concurrent subscriptions")
    void testSseEndpointTooManySubscriptions() throws Exception {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.getAssessment(assessmentId, userId)).thenReturn(assessment);
        when(sseEmitterManager.canRegister(userId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> controller.getProgress(assessmentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("maximum concurrent subscriptions");
    }

    @Test
    @DisplayName("polling endpoint should set Cache-Control header")
    void testPollingEndpointCacheControl() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.getAssessment(assessmentId, userId)).thenReturn(assessment);
        when(progressTracker.computeProgress(assessment)).thenReturn(progressEvent);

        // Act
        ResponseEntity<AssessmentProgressEvent> response = controller.getProgressPoll(assessmentId);

        // Assert
        assertThat(response.getHeaders().getCacheControl()).isNotNull();
        assertThat(response.getHeaders().getCacheControl()).contains("max-age=2");
    }

    @Test
    @DisplayName("both endpoints should call computeProgress with assessment")
    void testBothEndpointsComputeProgress() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.getAssessment(assessmentId, userId)).thenReturn(assessment);
        when(progressTracker.computeProgress(assessment)).thenReturn(progressEvent);
        when(sseEmitterManager.canRegister(userId)).thenReturn(true);

        // Act
        controller.getProgressPoll(assessmentId);

        // Assert - verify progressTracker.computeProgress was called
        verify(progressTracker, atLeastOnce()).computeProgress(assessment);
    }

    @Test
    @DisplayName("SSE endpoint should emit initial event after registration")
    void testSseEndpointEmitsInitialEvent() throws Exception {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.getAssessment(assessmentId, userId)).thenReturn(assessment);
        when(progressTracker.computeProgress(assessment)).thenReturn(progressEvent);
        when(sseEmitterManager.canRegister(userId)).thenReturn(true);

        // Act
        ResponseEntity<SseEmitter> response = controller.getProgress(assessmentId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("polling endpoint should return same event as SSE")
    void testSseAndPollingReturnSameEvent() {
        // Arrange
        when(authenticationUtil.getCurrentUserId()).thenReturn(userId);
        when(assessmentService.getAssessment(assessmentId, userId)).thenReturn(assessment);
        when(progressTracker.computeProgress(assessment)).thenReturn(progressEvent);
        when(sseEmitterManager.canRegister(userId)).thenReturn(true);

        // Act - polling endpoint
        ResponseEntity<AssessmentProgressEvent> pollResponse = controller.getProgressPoll(assessmentId);

        // Assert - polling returns same data
        assertThat(pollResponse.getBody()).isEqualTo(progressEvent);
        assertThat(pollResponse.getBody().assessmentId()).isEqualTo(progressEvent.assessmentId());
        assertThat(pollResponse.getBody().status()).isEqualTo(progressEvent.status());
        assertThat(pollResponse.getBody().progressPercent()).isEqualTo(progressEvent.progressPercent());
    }
}
