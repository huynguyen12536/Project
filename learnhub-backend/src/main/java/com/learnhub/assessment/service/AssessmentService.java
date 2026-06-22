package com.learnhub.assessment.service;

import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentResult;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.exception.AssessmentNotFoundException;
import com.learnhub.assessment.exception.DuplicateSubmissionException;
import com.learnhub.assessment.exception.TooManyActiveAssessmentsException;
import com.learnhub.assessment.repository.AssessmentRepository;
import com.learnhub.assessment.repository.AssessmentResultRepository;
import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.exception.SnapshotNotFoundException;
import com.learnhub.github.repository.RepositorySnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing assessment submissions and status updates.
 * Handles business logic for assessment lifecycle management.
 */
@Service
@Transactional
@Slf4j
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentResultRepository resultRepository;
    private final RepositorySnapshotRepository snapshotRepository;

    public AssessmentService(
        AssessmentRepository assessmentRepository,
        AssessmentResultRepository resultRepository,
        RepositorySnapshotRepository snapshotRepository
    ) {
        this.assessmentRepository = assessmentRepository;
        this.resultRepository = resultRepository;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Submit a new assessment for a repository snapshot.
     * Validates that the snapshot exists and is owned by the user.
     *
     * @param userId the ID of the user submitting the assessment
     * @param snapshotId the ID of the snapshot to assess
     * @return the created Assessment entity
     * @throws IllegalArgumentException if userId or snapshotId is null
     * @throws SnapshotNotFoundException if snapshot not found or not owned by user
     */
    public Assessment submitAssessment(UUID userId, UUID snapshotId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (snapshotId == null) {
            throw new IllegalArgumentException("Snapshot ID must not be null");
        }

        // Verify snapshot exists and belongs to user
        RepositorySnapshot snapshot = snapshotRepository.findByIdAndUserId(snapshotId, userId)
            .orElseThrow(() -> new SnapshotNotFoundException(
                "Snapshot not found or not owned by user: " + snapshotId
            ));

        Assessment assessment = new Assessment();
        assessment.setUserId(userId);
        assessment.setSnapshotId(snapshotId);
        assessment.setStatus(AssessmentStatus.PENDING);

        Assessment saved = assessmentRepository.save(assessment);
        log.info("Assessment submitted by user {} for snapshot {}", userId, snapshotId);
        return saved;
    }

    /**
     * Submit a new assessment with rate limiting validation.
     * Enforces:
     * - Max 3 concurrent PROCESSING assessments per user (409 if exceeded)
     * - Duplicate protection: reject if (userId, snapshotId) submitted < 60s ago (409 if duplicate)
     *
     * @param userId the ID of the user submitting the assessment
     * @param snapshotId the ID of the snapshot to assess
     * @return the created Assessment entity
     * @throws IllegalArgumentException if userId or snapshotId is null
     * @throws SnapshotNotFoundException if snapshot not found or not owned by user
     * @throws TooManyActiveAssessmentsException if user already has 3 concurrent PROCESSING assessments (409)
     * @throws DuplicateSubmissionException if (userId, snapshotId) submitted < 60s ago (409)
     */
    public Assessment submitAssessmentWithRateLimit(UUID userId, UUID snapshotId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (snapshotId == null) {
            throw new IllegalArgumentException("Snapshot ID must not be null");
        }

        // Verify snapshot exists and belongs to user
        RepositorySnapshot snapshot = snapshotRepository.findByIdAndUserId(snapshotId, userId)
            .orElseThrow(() -> new SnapshotNotFoundException(
                "Snapshot not found or not owned by user: " + snapshotId
            ));

        // Rate limit check 1: Max 3 concurrent PROCESSING assessments per user
        long activeCount = assessmentRepository.countByUserIdAndStatus(userId, AssessmentStatus.PROCESSING);
        if (activeCount >= 3) {
            log.warn("User {} exceeded max concurrent assessments ({} active)", userId, activeCount);
            throw new TooManyActiveAssessmentsException(
                "Max 3 concurrent assessments allowed. Please wait for existing assessments to complete."
            );
        }

        // Rate limit check 2: Duplicate submission protection (60 second window)
        Instant sixtySecondsAgo = Instant.now().minus(Duration.ofSeconds(60));
        Optional<Assessment> recentSubmission = assessmentRepository.findRecentByUserAndSnapshot(
            userId, snapshotId, sixtySecondsAgo
        );

        if (recentSubmission.isPresent()) {
            Assessment recent = recentSubmission.get();
            long secondsSinceSubmission = Duration.between(recent.getCreatedAt(), Instant.now()).getSeconds();
            int retryAfter = (int) Math.max(1, 60 - secondsSinceSubmission);

            log.warn("User {} attempted duplicate submission for snapshot {} ({} seconds ago)",
                userId, snapshotId, secondsSinceSubmission);
            throw new DuplicateSubmissionException(
                "Assessment for this repository was submitted " + secondsSinceSubmission +
                " seconds ago. Please wait " + retryAfter + " seconds before resubmitting.",
                retryAfter
            );
        }

        // All checks passed, create assessment
        Assessment assessment = new Assessment();
        assessment.setUserId(userId);
        assessment.setSnapshotId(snapshotId);
        assessment.setStatus(AssessmentStatus.PENDING);

        Assessment saved = assessmentRepository.save(assessment);
        log.info("Assessment submitted with rate limit validation by user {} for snapshot {}", userId, snapshotId);
        return saved;
    }

    /**
     * Get an assessment by ID, verifying ownership.
     *
     * @param assessmentId the ID of the assessment
     * @param userId the ID of the user (for ownership verification)
     * @return the Assessment entity
     * @throws IllegalArgumentException if assessmentId or userId is null
     * @throws AssessmentNotFoundException if assessment not found or not owned by user
     */
    @Transactional(readOnly = true)
    public Assessment getAssessment(UUID assessmentId, UUID userId) {
        if (assessmentId == null || userId == null) {
            throw new IllegalArgumentException("Assessment ID and User ID must not be null");
        }

        return assessmentRepository.findByIdAndUserId(assessmentId, userId)
            .orElseThrow(() -> new AssessmentNotFoundException(
                "Assessment not found or not owned by user: " + assessmentId
            ));
    }

    /**
     * Get all assessments for a user, ordered by creation date (newest first).
     *
     * @param userId the ID of the user
     * @return list of Assessment entities
     */
    @Transactional(readOnly = true)
    public List<Assessment> getUserAssessments(UUID userId) {
        return assessmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Update the status of an assessment.
     * Sets completedAt timestamp when status changes to COMPLETED or FAILED.
     * Note: This method is designed for internal use by the analysis engine.
     *
     * @param assessmentId the ID of the assessment
     * @param status the new status
     * @return the updated Assessment entity
     * @throws IllegalArgumentException if assessmentId or status is null
     * @throws AssessmentNotFoundException if assessment not found
     */
    public Assessment updateStatus(UUID assessmentId, AssessmentStatus status) {
        if (assessmentId == null || status == null) {
            throw new IllegalArgumentException("Assessment ID and status must not be null");
        }

        Assessment assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new AssessmentNotFoundException(
                "Assessment not found: " + assessmentId
            ));

        assessment.setStatus(status);

        // Set completion timestamp when assessment is finalized
        if (status == AssessmentStatus.COMPLETED || status == AssessmentStatus.FAILED) {
            assessment.setCompletedAt(Instant.now());
        }

        Assessment updated = assessmentRepository.save(assessment);
        log.info("Assessment {} status updated to {}", assessmentId, status);
        return updated;
    }

    /**
     * Save assessment results from scoring engine.
     * Called by AssessmentJobConsumer after evaluation completes.
     *
     * @param assessmentId the ID of the assessment
     * @param result the result from CompetencyScoringEngine
     * @return the saved AssessmentResult entity
     * @throws AssessmentNotFoundException if assessment not found
     */
    public AssessmentResult saveAssessmentResult(UUID assessmentId, AssessmentResult result) {
        if (assessmentId == null) {
            throw new IllegalArgumentException("Assessment ID must not be null");
        }
        if (result == null) {
            throw new IllegalArgumentException("Assessment result must not be null");
        }

        // Verify assessment exists
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(
                        "Assessment not found: " + assessmentId
                ));

        result.setAssessmentId(assessmentId);
        AssessmentResult saved = resultRepository.save(result);
        log.info("Assessment result saved: assessmentId={}, overallLevel={}",
                assessmentId, result.getOverallLevel());
        return saved;
    }

    /**
     * Get assessment result by assessment ID.
     *
     * @param assessmentId the ID of the assessment
     * @return Optional containing the result if it exists
     */
    @Transactional(readOnly = true)
    public Optional<AssessmentResult> getAssessmentResult(UUID assessmentId) {
        return resultRepository.findByAssessmentId(assessmentId);
    }

    /**
     * Get assessment with result, verifying ownership.
     *
     * @param assessmentId the ID of the assessment
     * @param userId the ID of the user (for ownership verification)
     * @return AssessmentDetailsResponse with assessment and result
     * @throws AssessmentNotFoundException if assessment not found or not owned by user
     */
    @Transactional(readOnly = true)
    public AssessmentWithResult getAssessmentWithResult(UUID assessmentId, UUID userId) {
        if (assessmentId == null || userId == null) {
            throw new IllegalArgumentException("Assessment ID and User ID must not be null");
        }

        Assessment assessment = assessmentRepository.findByIdAndUserId(assessmentId, userId)
                .orElseThrow(() -> new AssessmentNotFoundException(
                        "Assessment not found or not owned by user: " + assessmentId
                ));

        Optional<AssessmentResult> result = resultRepository.findByAssessmentId(assessmentId);

        return new AssessmentWithResult(assessment, result.orElse(null));
    }

    /**
     * Helper class to bundle Assessment and optional AssessmentResult.
     */
    public static class AssessmentWithResult {
        public final Assessment assessment;
        public final AssessmentResult result;

        public AssessmentWithResult(Assessment assessment, AssessmentResult result) {
            this.assessment = assessment;
            this.result = result;
        }
    }
}
