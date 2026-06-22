package com.learnhub.assessment.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentResult;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.queue.AssessmentJob;
import com.learnhub.assessment.queue.AssessmentJobQueue;
import com.learnhub.assessment.repository.AssessmentRepository;
import com.learnhub.assessment.service.AssessmentService;
import com.learnhub.brf.service.BrfService;
import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.service.RepositorySnapshotService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Redis-based assessment job consumer.
 *
 * Continuously polls the assessment_queue in Redis, dequeues jobs,
 * and orchestrates repository evaluation through the assessment engine.
 *
 * Job Flow:
 * 1. AssessmentController enqueues job to Redis (assessment_queue)
 * 2. JobConsumer dequeues and calls processJob()
 * 3. ProcessJob loads repo snapshot and BRF version
 * 4. AssessmentEngine evaluates against BRF rules
 * 5. Results persisted and Assessment marked COMPLETED
 * 6. Learner can view results via REST API
 *
 * Error Handling:
 * - Failed jobs moved to assessment_dlq for manual review
 * - Retry logic: 3 attempts before giving up
 * - Partial failures: Skip failed detector, continue with others
 */
@Component
@Slf4j
public class AssessmentJobConsumer {

    private final AssessmentJobQueue jobQueue;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentService assessmentService;
    private final RepositorySnapshotService snapshotService;
    private final AssessmentEngine engine;
    private final BrfService brfService;
    private final ObjectMapper objectMapper;

    @Value("${assessment.job-poll-timeout-seconds:1}")
    private int pollTimeoutSeconds;

    @Value("${assessment.max-retries:3}")
    private int maxRetries;

    /**
     * Create assessment job consumer.
     *
     * @param jobQueue Redis-based job queue
     * @param assessmentRepository repository for assessment entities
     * @param assessmentService service for managing assessments
     * @param snapshotService service for loading repository snapshots
     * @param engine core assessment evaluation engine
     * @param brfService service for loading BRF rules
     * @param objectMapper JSON serialization
     */
    public AssessmentJobConsumer(
            AssessmentJobQueue jobQueue,
            AssessmentRepository assessmentRepository,
            AssessmentService assessmentService,
            RepositorySnapshotService snapshotService,
            AssessmentEngine engine,
            BrfService brfService,
            ObjectMapper objectMapper) {
        this.jobQueue = jobQueue;
        this.assessmentRepository = assessmentRepository;
        this.assessmentService = assessmentService;
        this.snapshotService = snapshotService;
        this.engine = engine;
        this.brfService = brfService;
        this.objectMapper = objectMapper;
    }

    /**
     * Start consumer on Spring initialization.
     * Spawns background thread for continuous polling.
     */
    @PostConstruct
    public void start() {
        new Thread(this::pollAndProcess, "AssessmentJobConsumer-Thread")
                .start();
        log.info("AssessmentJobConsumer started");
    }

    /**
     * Main consumer loop: continuously poll Redis queue and process jobs.
     *
     * Uses BRPOP (blocking right pop) for efficient polling with timeout.
     * No busy-waiting; blocks until job available or timeout expires.
     *
     * Loop continues indefinitely to consume all queued jobs.
     * Errors are logged but don't stop the consumer.
     */
    private void pollAndProcess() {
        Duration pollTimeout = Duration.ofSeconds(pollTimeoutSeconds);

        while (true) {
            try {
                // Blocking pop with timeout (returns empty Optional after timeout)
                Optional<AssessmentJob> jobOpt = jobQueue.dequeue(pollTimeout);

                if (jobOpt.isPresent()) {
                    processJobWithRetry(jobOpt.get());
                }
                // If empty, timeout occurred; loop continues and tries again
            } catch (Exception e) {
                log.error("Unexpected error in job consumer loop", e);
                // Continue polling even on error; don't let exceptions kill consumer
            }
        }
    }

    /**
     * Process job with automatic retry logic.
     *
     * Retries job up to maxRetries times before moving to dead-letter queue.
     * Transient failures (network, timeout) may succeed on retry.
     * Uses exponential backoff: 1s, 2s, 4s between retries.
     *
     * @param job the assessment job to process
     */
    private void processJobWithRetry(AssessmentJob job) {
        int maxAttempts = maxRetries;

        while (job.getRetryCount() < maxAttempts) {
            try {
                processJob(job);
                log.info("Job processed successfully: assessmentId={}", job.getAssessmentId());
                return;  // Success
            } catch (Exception e) {
                int nextRetryCount = job.getRetryCount() + 1;
                log.warn("Job processing failed (attempt {}/{}): assessmentId={}, error={}",
                        nextRetryCount, maxAttempts, job.getAssessmentId(), e.getMessage());

                if (nextRetryCount >= maxAttempts) {
                    // All retries exhausted; move to DLQ
                    log.error("Job failed after {} attempts, moving to DLQ: assessmentId={}",
                            maxAttempts, job.getAssessmentId());
                    job.setFailureReason(e.getMessage());
                    try {
                        jobQueue.moveToDeadLetterQueue(job, e.getMessage());
                    } catch (Exception dlqError) {
                        log.error("Failed to move job to DLQ: assessmentId={}",
                                job.getAssessmentId(), dlqError);
                    }
                    return;
                } else {
                    // Retry after exponential backoff
                    long backoffMs = (long) Math.pow(2, job.getRetryCount()) * 1000;
                    log.debug("Retrying after {}ms backoff: assessmentId={}",
                            backoffMs, job.getAssessmentId());
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Retry sleep interrupted: assessmentId={}",
                                job.getAssessmentId());
                        return;
                    }

                    // Requeue with incremented retry count
                    job = job.withIncrementedRetry();
                    jobQueue.requeue(job);
                }
            }
        }
    }

    /**
     * Core job processing logic.
     *
     * Steps:
     * 1. Fetch Assessment record, update status to PROCESSING
     * 2. Load repository snapshot (git archive)
     * 3. Load BRF rules by version
     * 4. Invoke AssessmentEngine to evaluate
     * 5. Persist results and mark COMPLETED
     *
     * @param job the assessment job to process
     * @throws Exception if processing fails (will trigger retry logic)
     */
    private void processJob(AssessmentJob job) throws Exception {
        log.debug("Processing job: assessmentId={}, repoId={}, brfVersion={}, retryCount={}",
                job.getAssessmentId(), job.getRepoId(), job.getBrfVersion(),
                job.getRetryCount());

        // Fetch assessment record
        Assessment assessment = assessmentRepository.findById(job.getAssessmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assessment not found: " + job.getAssessmentId()));

        // Update status to PROCESSING
        assessment.setStatus(AssessmentStatus.PROCESSING);
        assessment.setStartedAt(Instant.now());
        assessmentRepository.save(assessment);
        log.debug("Assessment status updated to PROCESSING: {}", job.getAssessmentId());

        try {
            // Load repository snapshot
            RepositorySnapshot snapshot = snapshotService.getSnapshot(job.getRepoId());
            if (snapshot == null) {
                throw new IllegalStateException(
                        "Repository snapshot not found: " + job.getRepoId());
            }
            log.debug("Repository snapshot loaded: {}", job.getRepoId());

            // Load BRF rules by version
            String brfVersion = job.getBrfVersion() != null ?
                    job.getBrfVersion() : brfService.getDefaultVersion();
            if (!brfService.versionExists(brfVersion)) {
                throw new IllegalStateException(
                        "BRF version not found: " + brfVersion);
            }
            log.debug("BRF version validated: {}", brfVersion);

            // Evaluate repository against BRF
            CompetencyScoringEngine.AssessmentResult evalResult = engine.evaluate(snapshot, brfVersion);
            log.debug("Assessment evaluation completed: assessmentId={}",
                    job.getAssessmentId());

            // Persist assessment with status update
            assessment.setStatus(AssessmentStatus.COMPLETED);
            assessment.setCompletedAt(Instant.now());
            assessment.setResultJson(objectMapper.writeValueAsString(evalResult));
            assessmentRepository.save(assessment);

            // Persist detailed results
            AssessmentResult detailedResult = AssessmentResult.builder()
                    .assessmentId(job.getAssessmentId())
                    .overallLevel(evalResult.overallLevel.name())
                    .allGaps(evalResult.gaps)
                    .nextSteps(evalResult.nextSteps)
                    .overallConfidence(evalResult.overallConfidence)
                    .resultsJson(objectMapper.writeValueAsString(evalResult))
                    .build();
            assessmentService.saveAssessmentResult(job.getAssessmentId(), detailedResult);

            log.info("Job completed successfully: assessmentId={}, overallLevel={}",
                    job.getAssessmentId(), evalResult.overallLevel);

        } catch (Exception e) {
            // Mark assessment as failed and log error
            assessment.setStatus(AssessmentStatus.FAILED);
            assessment.setErrorMessage(e.getMessage());
            assessmentRepository.save(assessment);
            log.error("Assessment evaluation failed: assessmentId={}",
                    job.getAssessmentId(), e);
            throw e;  // Re-throw to trigger retry logic
        }
    }
}
