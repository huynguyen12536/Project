package com.learnhub.assessment.controller;

import com.learnhub.assessment.dto.AssessmentProgressEvent;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.service.AssessmentService;
import com.learnhub.assessment.service.ProgressTracker;
import com.learnhub.assessment.util.SseEmitterManager;
import com.learnhub.common.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * REST Controller for real-time assessment progress tracking.
 * Provides both SSE (push) and polling (pull) endpoints for progress updates.
 *
 * SSE Endpoint: GET /api/v1/assessments/{id}/progress
 *   - Returns event stream with Content-Type: text/event-stream
 *   - Emits AssessmentProgressEvent every 1-2 seconds
 *   - Auto-closes after 10 minutes of inactivity
 *
 * Polling Endpoint: GET /api/v1/assessments/{id}/progress/poll
 *   - Returns JSON with latest AssessmentProgressEvent
 *   - Sets Cache-Control: max-age=2 seconds
 *   - Returns 204 No Content if no change since last poll (MVP: not implemented)
 */
@RestController
@RequestMapping("/api/v1/assessments")
@PreAuthorize("hasRole('LEARNER')")
@Slf4j
public class AssessmentProgressController {

    private final AssessmentService assessmentService;
    private final ProgressTracker progressTracker;
    private final SseEmitterManager sseEmitterManager;
    private final AuthenticationUtil authenticationUtil;

    private static final long SSE_TIMEOUT_MILLIS = 600_000L; // 10 minutes
    private static final long POLLING_CACHE_SECONDS = 2L; // 2 seconds

    public AssessmentProgressController(
        AssessmentService assessmentService,
        ProgressTracker progressTracker,
        SseEmitterManager sseEmitterManager,
        AuthenticationUtil authenticationUtil
    ) {
        this.assessmentService = assessmentService;
        this.progressTracker = progressTracker;
        this.sseEmitterManager = sseEmitterManager;
        this.authenticationUtil = authenticationUtil;
    }

    /**
     * SSE Endpoint: Stream assessment progress updates.
     * Returns Server-Sent Events stream with real-time progress.
     *
     * @param id the assessment ID
     * @return SseEmitter for streaming progress events
     * @throws IllegalStateException if user exceeds max concurrent SSE subscriptions
     */
    @GetMapping("/{id}/progress")
    @Operation(summary = "Get assessment progress via SSE stream")
    @ApiResponse(responseCode = "200", description = "SSE stream established")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Assessment not found")
    @ApiResponse(responseCode = "409", description = "Too many concurrent subscriptions")
    public ResponseEntity<SseEmitter> getProgress(
        @PathVariable UUID id
    ) {
        log.info("SSE subscription requested for assessment {}", id);
        UUID userId = authenticationUtil.getCurrentUserId();

        // Verify ownership and get assessment
        Assessment assessment = assessmentService.getAssessment(id, userId);

        // Check if user can register another SSE subscription
        if (!sseEmitterManager.canRegister(userId)) {
            log.warn("User {} exceeded max concurrent SSE subscriptions", userId);
            throw new IllegalStateException(
                "User exceeds maximum concurrent subscriptions. Please close other connections."
            );
        }

        // Create SSE emitter with 10-minute timeout
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        // Register emitter (this sets up timeout/completion handlers)
        sseEmitterManager.register(id, userId, emitter);
        log.debug("SSE emitter registered for assessment {}, user {}", id, userId);

        // Compute and emit initial progress event
        AssessmentProgressEvent initialEvent = progressTracker.computeProgress(assessment);
        try {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .id(UUID.randomUUID().toString())
                .name("status")
                .data(initialEvent)
                .reconnectTime(2000L); // 2 second reconnect time

            emitter.send(eventBuilder);
            log.debug("Initial progress event sent for assessment {}", id);
        } catch (IOException e) {
            log.warn("Failed to send initial progress event for assessment {}: {}", id, e.getMessage());
            sseEmitterManager.deregister(id, userId);
            throw new IllegalStateException("Failed to establish SSE connection");
        }

        // Return SseEmitter (Spring handles the stream response)
        return ResponseEntity
            .ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(emitter);
    }

    /**
     * Polling Endpoint: Get latest assessment progress.
     * Returns current progress state as JSON for clients that cannot use SSE.
     *
     * @param id the assessment ID
     * @return Latest AssessmentProgressEvent as JSON
     */
    @GetMapping("/{id}/progress/poll")
    @Operation(summary = "Get assessment progress via polling")
    @ApiResponse(responseCode = "200", description = "Progress event returned")
    @ApiResponse(responseCode = "204", description = "No change since last poll (MVP: not implemented)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Assessment not found")
    public ResponseEntity<AssessmentProgressEvent> getProgressPoll(
        @PathVariable UUID id
    ) {
        log.info("Polling progress requested for assessment {}", id);
        UUID userId = authenticationUtil.getCurrentUserId();

        // Verify ownership and get assessment
        Assessment assessment = assessmentService.getAssessment(id, userId);

        // Compute current progress
        AssessmentProgressEvent progressEvent = progressTracker.computeProgress(assessment);
        log.debug("Progress polled for assessment {}: status={}, progress={}%",
            id, progressEvent.status(), progressEvent.progressPercent());

        // Return with Cache-Control header (2 second max-age)
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(POLLING_CACHE_SECONDS, TimeUnit.SECONDS))
            .body(progressEvent);
    }
}
