package com.learnhub.assessment.controller;

import com.learnhub.assessment.dto.AssessmentDetailsResponse;
import com.learnhub.assessment.dto.AssessmentListResponse;
import com.learnhub.assessment.dto.AssessmentResponse;
import com.learnhub.assessment.dto.SubmitAssessmentRequest;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.exception.AssessmentNotFoundException;
import com.learnhub.assessment.queue.AssessmentJob;
import com.learnhub.assessment.queue.AssessmentJobQueue;
import com.learnhub.assessment.service.AssessmentService;
import com.learnhub.common.util.AuthenticationUtil;
import com.learnhub.github.exception.SnapshotNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for assessment operations.
 * Handles submission and retrieval of code assessments.
 */
@RestController
@RequestMapping("/api/v1/assessments")
@PreAuthorize("hasRole('LEARNER')")
@Slf4j
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AuthenticationUtil authenticationUtil;
    private final AssessmentJobQueue jobQueue;

    public AssessmentController(
        AssessmentService assessmentService,
        AuthenticationUtil authenticationUtil
    ) {
        this.assessmentService = assessmentService;
        this.authenticationUtil = authenticationUtil;
        this.jobQueue = null; // Will be injected if available
    }

    public AssessmentController(
        AssessmentService assessmentService,
        AuthenticationUtil authenticationUtil,
        AssessmentJobQueue jobQueue
    ) {
        this.assessmentService = assessmentService;
        this.authenticationUtil = authenticationUtil;
        this.jobQueue = jobQueue;
    }

    /**
     * Submit a new assessment for a repository snapshot.
     * Returns immediately (< 50ms) with PENDING status.
     * Actual assessment processing is enqueued asynchronously.
     *
     * The snapshot must be owned by the authenticated user.
     *
     * @param request the assessment submission request containing snapshot ID
     * @return 201 Created with the created assessment details (status: PENDING)
     * @throws SnapshotNotFoundException if snapshot not found or not owned by user
     */
    @PostMapping
    @Operation(summary = "Submit a new assessment")
    @ApiResponse(responseCode = "201", description = "Assessment submitted (status: PENDING)")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Snapshot not found")
    @ApiResponse(responseCode = "409", description = "Duplicate submission or too many active assessments")
    public ResponseEntity<AssessmentResponse> submitAssessment(
        @RequestBody @Valid SubmitAssessmentRequest request
    ) {
        log.info("Submitting assessment for snapshot {}", request.getSnapshotId());
        UUID userId = authenticationUtil.getCurrentUserId();

        try {
            // Create assessment (synchronous, < 20ms)
            Assessment assessment = assessmentService.submitAssessment(userId, request.getSnapshotId());

            // Enqueue job asynchronously (fire-and-forget, does not block)
            enqueueAssessmentJobAsync(assessment.getId(), userId, request.getSnapshotId());

            // Return immediately with PENDING status
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(AssessmentResponse.fromEntity(assessment));
        } catch (SnapshotNotFoundException e) {
            log.warn("Snapshot not found: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Asynchronously enqueue assessment job for processing.
     * Does NOT block the controller thread. Errors are logged but not propagated.
     *
     * @param assessmentId the assessment ID
     * @param userId the user ID
     * @param snapshotId the snapshot ID
     */
    private void enqueueAssessmentJobAsync(UUID assessmentId, UUID userId, UUID snapshotId) {
        // Fire-and-forget: do NOT use @Async here to avoid thread pool overhead
        // Instead, use try-catch to handle queue failures gracefully
        try {
            if (jobQueue != null) {
                // Create job with default BRF version
                AssessmentJob job = new AssessmentJob(assessmentId, snapshotId, "1.0.0");

                jobQueue.enqueue(job);
                log.info("Assessment job enqueued: assessmentId={}, userId={}", assessmentId, userId);
            } else {
                log.warn("Job queue not available, assessment job not enqueued: assessmentId={}", assessmentId);
            }
        } catch (Exception e) {
            // Log error but do NOT throw (fire-and-forget pattern)
            // Assessment already created with PENDING status, so user is not blocked
            log.error("Failed to enqueue assessment job: assessmentId={}, error={}", assessmentId, e.getMessage(), e);
        }
    }

    /**
     * Get an assessment by ID.
     * User can only access assessments they own.
     *
     * @param id the assessment ID
     * @return 200 OK with the assessment details
     * @throws AssessmentNotFoundException if assessment not found or not owned by user
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get assessment by ID")
    @ApiResponse(responseCode = "200", description = "Assessment found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Assessment not found")
    public ResponseEntity<AssessmentResponse> getAssessment(
        @PathVariable UUID id
    ) {
        log.info("Retrieving assessment {}", id);
        UUID userId = authenticationUtil.getCurrentUserId();

        try {
            Assessment assessment = assessmentService.getAssessment(id, userId);
            return ResponseEntity.ok(AssessmentResponse.fromEntity(assessment));
        } catch (AssessmentNotFoundException e) {
            log.warn("Assessment not found: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get all assessments for the authenticated user.
     * Results are ordered by creation date (newest first).
     *
     * @return 200 OK with list of assessments
     */
    @GetMapping
    @Operation(summary = "Get user's assessments")
    @ApiResponse(responseCode = "200", description = "Assessments found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<AssessmentListResponse> getUserAssessments() {
        log.info("Retrieving assessments for user");
        UUID userId = authenticationUtil.getCurrentUserId();

        List<Assessment> assessments = assessmentService.getUserAssessments(userId);
        return ResponseEntity.ok(AssessmentListResponse.fromEntities(assessments));
    }

    /**
     * Get detailed assessment results including scoring and feedback.
     * Only returns results if assessment is COMPLETED.
     * User can only access assessments they own.
     *
     * @param id the assessment ID
     * @return 200 OK with assessment details and results
     * @throws AssessmentNotFoundException if assessment not found or not owned by user
     */
    @GetMapping("/{id}/details")
    @Operation(summary = "Get detailed assessment results")
    @ApiResponse(responseCode = "200", description = "Assessment details found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Assessment not found")
    public ResponseEntity<AssessmentDetailsResponse> getAssessmentDetails(
            @PathVariable UUID id
    ) {
        log.info("Retrieving assessment details for {}", id);
        UUID userId = authenticationUtil.getCurrentUserId();

        try {
            AssessmentService.AssessmentWithResult withResult =
                    assessmentService.getAssessmentWithResult(id, userId);

            return ResponseEntity.ok(
                    AssessmentDetailsResponse.fromEntities(
                            withResult.assessment,
                            withResult.result
                    )
            );
        } catch (AssessmentNotFoundException e) {
            log.warn("Assessment not found: {}", e.getMessage());
            throw e;
        }
    }
}
