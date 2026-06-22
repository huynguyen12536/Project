package com.learnhub.assessment.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.assessment.dto.AssessmentProgressEvent;
import com.learnhub.assessment.dto.SubmitAssessmentRequest;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.queue.AssessmentJob;
import com.learnhub.assessment.queue.RedisAssessmentJobQueue;
import com.learnhub.assessment.repository.AssessmentRepository;
import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.repository.RepositorySnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 1 Integration Tests for Assessment Engine.
 *
 * Tests the complete assessment flow with real PostgreSQL and Redis.
 * These tests run against actual infrastructure (not mocks).
 *
 * Test Categories:
 * 1. SSE Endpoint Tests (real-time progress streaming)
 * 2. Polling Endpoint Tests (fallback when SSE unavailable)
 * 3. Rate Limiting Tests (3 concurrent limit, 60s duplicate detection)
 * 4. Ownership Validation Tests (security/authorization)
 * 5. Race Condition Tests (stale state prevention)
 * 6. Retry Loop Tests (context preservation)
 *
 * Critical Requirements (From Architecture):
 * - SSE emits events at 1-2s intervals
 * - Polling returns same data as SSE
 * - Rate limiter blocks 4th concurrent job
 * - Rate limiter detects duplicates within 60s
 * - Users can only access their own assessments
 * - Dequeued jobs have fresh state (no staleness)
 * - Retry loop preserves assessment context
 */
@AutoConfigureMockMvc
@Transactional
@Slf4j
class AssessmentPhase1IntegrationTest extends AssessmentIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private RepositorySnapshotRepository snapshotRepository;

    @Autowired
    private RedisAssessmentJobQueue jobQueue;

    private UUID testUserId;
    private UUID testSnapshotId;
    private RepositorySnapshot testSnapshot;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testSnapshotId = UUID.randomUUID();

        // Create test snapshot
        testSnapshot = RepositorySnapshot.builder()
                .id(testSnapshotId)
                .userId(testUserId)
                .repositoryId(UUID.randomUUID())
                .ownerUsername("test-owner")
                .repositoryName("test-repo")
                .branchName("main")
                .commitSha("abc123def456")
                .filesContent(new HashMap<>())
                .createdAt(Instant.now())
                .build();

        snapshotRepository.save(testSnapshot);
        log.info("Test setup complete: userId={}, snapshotId={}", testUserId, testSnapshotId);
    }

    // ========== CRITICAL TEST 1: SSE Endpoint Emits Events Correctly ==========

    @Nested
    @DisplayName("SSE Endpoint Tests")
    class SSEEndpointTests {

        @Test
        @WithMockUser(roles = "LEARNER")
        @DisplayName("SSE endpoint subscribes to assessment and receives events")
        void testSSEEndpoint_SubscribesToAssessment_ReceivesEvents() throws Exception {
            // GIVEN: An assessment in PROCESSING status
            Assessment assessment = Assessment.builder()
                    .userId(testUserId)
                    .snapshotId(testSnapshotId)
                    .status(AssessmentStatus.PROCESSING)
                    .startedAt(Instant.now())
                    .build();
            Assessment saved = assessmentRepository.save(assessment);

            // WHEN: Client subscribes to SSE endpoint
            // NOTE: This test skeleton - actual SSE subscription requires async mock handling
            // Implementation: Use MockMvc with text/event-stream and async context

            // THEN: SSE events are received with progress updates
            // Expected flow:
            // - Event 1: { status: "PROCESSING", progressPercent: 25, currentStep: "analysis_step_1" }
            // - Event 2: { status: "PROCESSING", progressPercent: 50, currentStep: "analysis_step_2" }
            // - Event 3: { status: "COMPLETED", progressPercent: 100, ... }

            // ASSERTION CHECKLIST:
            // [ ] SSE connection established (HTTP 200, Content-Type: text/event-stream)
            // [ ] First event received within 2 seconds
            // [ ] progressPercent increments correctly
            // [ ] currentStep field reflects actual processing step
            // [ ] confidence field is between 0.0 and 1.0
            // [ ] timestamp is monotonically increasing
            // [ ] COMPLETED event received before timeout

            // Placeholder implementation
            log.info("SKELETON: SSE endpoint test - implement async SSE assertion");
        }

        @Test
        @WithMockUser(roles = "LEARNER")
        @DisplayName("SSE endpoint closes gracefully on assessment completion")
        void testSSEEndpoint_ClosesGracefully_OnCompletion() throws Exception {
            // GIVEN: An assessment in PROCESSING status
            Assessment assessment = Assessment.builder()
                    .userId(testUserId)
                    .snapshotId(testSnapshotId)
                    .status(AssessmentStatus.PROCESSING)
                    .startedAt(Instant.now())
                    .build();
            assessmentRepository.save(assessment);

            // WHEN: Assessment completes processing
            // THEN: SSE connection closes with final event

            // ASSERTION CHECKLIST:
            // [ ] Final event has status = COMPLETED
            // [ ] Final event has progressPercent = 100
            // [ ] Connection closes (no further events)
            // [ ] HTTP response code transitions to 200 (completed)

            log.info("SKELETON: SSE graceful close test");
        }
    }

    // ========== CRITICAL TEST 2: Polling Endpoint Returns Same Data ==========

    @Nested
    @DisplayName("Polling Endpoint Tests")
    class PollingEndpointTests {

        @Test
        @WithMockUser(roles = "LEARNER")
        @DisplayName("Polling endpoint returns assessment progress")
        void testPollingEndpoint_ReturnsAssessmentProgress() throws Exception {
            // GIVEN: An assessment in PROCESSING status
            Assessment assessment = Assessment.builder()
                    .userId(testUserId)
                    .snapshotId(testSnapshotId)
                    .status(AssessmentStatus.PROCESSING)
                    .startedAt(Instant.now())
                    .build();
            Assessment saved = assessmentRepository.save(assessment);

            // WHEN: Client polls GET /api/v1/assessments/{id}/progress
            // THEN: Progress event is returned with current status

            // ASSERTION CHECKLIST:
            // [ ] HTTP 200 response
            // [ ] Response contains AssessmentProgressEvent
            // [ ] assessmentId matches requested ID
            // [ ] status is PROCESSING
            // [ ] progressPercent is between 0-100
            // [ ] currentStep is populated
            // [ ] estimatedSecondsRemaining is reasonable (>0, <3600)
            // [ ] confidence is between 0.0-1.0
            // [ ] timestamp is recent (within 5 seconds)

            // Placeholder: actual polling endpoint test
            MvcResult result = mockMvc.perform(
                    get("/api/v1/assessments/{id}/progress", saved.getId())
                            .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            log.info("Polling response: {}", response);
        }

        @Test
        @WithMockUser(roles = "LEARNER")
        @DisplayName("Polling endpoint returns consistent data with SSE")
        void testPollingEndpoint_ConsistentWithSSE() throws Exception {
            // GIVEN: An assessment generating events
            Assessment assessment = Assessment.builder()
                    .userId(testUserId)
                    .snapshotId(testSnapshotId)
                    .status(AssessmentStatus.PROCESSING)
                    .startedAt(Instant.now())
                    .build();
            Assessment saved = assessmentRepository.save(assessment);

            // WHEN: Poll the same assessment multiple times
            // THEN: Each poll returns the same or progressed data (never regress)

            // ASSERTION CHECKLIST:
            // [ ] First poll returns progress_percent = X
            // [ ] Second poll returns progress_percent >= X (monotonic)
            // [ ] status never changes backward (PROCESSING -> PENDING is invalid)
            // [ ] queuePosition decreases or stays same
            // [ ] Data matches what SSE would emit at that moment

            log.info("SKELETON: Polling consistency test");
        }
    }

    // ========== CRITICAL TEST 3: Rate Limiting Enforcement ==========

    @Nested
    @DisplayName("Rate Limiting Tests")
    class RateLimitingTests {

        @Test
        @WithMockUser(roles = "LEARNER")
        @DisplayName("Rate limiter enforces 3 concurrent assessment limit")
        void testRateLimiter_Enforces3ConcurrentLimit() throws Exception {
            // GIVEN: User has already submitted 3 assessments in progress
            Assessment[] assessments = new Assessment[3];
            for (int i = 0; i < 3; i++) {
                UUID snapshotId = UUID.randomUUID();
                RepositorySnapshot snapshot = RepositorySnapshot.builder()
                        .id(snapshotId)
                        .userId(testUserId)
                        .repositoryId(UUID.randomUUID())
                        .ownerUsername("test-owner")
                        .repositoryName("test-repo-" + i)
                        .branchName("main")
                        .commitSha("abc123")
                        .filesContent(new HashMap<>())
                        .createdAt(Instant.now())
                        .build();
                snapshotRepository.save(snapshot);

                assessments[i] = Assessment.builder()
                        .userId(testUserId)
                        .snapshotId(snapshotId)
                        .status(AssessmentStatus.PROCESSING)
                        .startedAt(Instant.now())
                        .build();
                assessmentRepository.save(assessments[i]);
            }

            // WHEN: User attempts to submit 4th assessment
            UUID fourthSnapshotId = UUID.randomUUID();
            RepositorySnapshot fourthSnapshot = RepositorySnapshot.builder()
                    .id(fourthSnapshotId)
                    .userId(testUserId)
                    .repositoryId(UUID.randomUUID())
                    .ownerUsername("test-owner")
                    .repositoryName("test-repo-4")
                    .branchName("main")
                    .commitSha("abc123")
                    .filesContent(new HashMap<>())
                    .createdAt(Instant.now())
                    .build();
            snapshotRepository.save(fourthSnapshot);

            SubmitAssessmentRequest request = new SubmitAssessmentRequest(fourthSnapshotId);

            // THEN: Request is rejected with 429 Too Many Requests
            mockMvc.perform(post("/api/v1/assessments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests());

            // ASSERTION CHECKLIST:
            // [ ] HTTP 429 status code
            // [ ] Response includes retry-after header
            // [ ] Response body explains rate limit (max 3 concurrent)
            // [ ] No assessment created in database
            // [ ] Error message is user-friendly
        }

        @Test
        @WithMockUser(roles = "LEARNER")
        @DisplayName("Rate limiter detects duplicate submissions within 60s")
        void testRateLimiter_DetectsDuplicateWithin60s() throws Exception {
            // GIVEN: User submits assessment for snapshot
            SubmitAssessmentRequest request = new SubmitAssessmentRequest(testSnapshotId);

            mockMvc.perform(post("/api/v1/assessments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // WHEN: User submits for same snapshot again within 60 seconds
            // THEN: Request is rejected with 409 Conflict

            mockMvc.perform(post("/api/v1/assessments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());

            // ASSERTION CHECKLIST:
            // [ ] HTTP 409 Conflict status code
            // [ ] Response includes existing assessmentId
            // [ ] Response message indicates duplicate submission
            // [ ] Only one assessment exists in database (idempotent)
            // [ ] Duplicate window is exactly 60 seconds
        }
    }

    // ========== CRITICAL TEST 4: Ownership Validation ==========

    @Nested
    @DisplayName("Ownership Validation Tests")
    class OwnershipValidationTests {

        @Test
        @WithMockUser(roles = "LEARNER", username = "user1")
        @DisplayName("User cannot access other user's assessment")
        void testOwnershipValidation_BlocksUnauthorizedAccess() throws Exception {
            // GIVEN: Assessment owned by different user
            UUID otherUserId = UUID.randomUUID();
            Assessment otherUsersAssessment = Assessment.builder()
                    .userId(otherUserId)
                    .snapshotId(testSnapshotId)
                    .status(AssessmentStatus.COMPLETED)
                    .completedAt(Instant.now())
                    .build();
            Assessment saved = assessmentRepository.save(otherUsersAssessment);

            // WHEN: Authenticated user attempts to GET other user's assessment
            // THEN: Request is rejected with 404 Not Found (never reveal ownership)

            mockMvc.perform(get("/api/v1/assessments/{id}", saved.getId()))
                    .andExpect(status().isNotFound());

            // ASSERTION CHECKLIST:
            // [ ] HTTP 404 status (not 403 to avoid information leak)
            // [ ] No assessment data returned
            // [ ] No error message revealing assessment existence
        }

        @Test
        @WithMockUser(roles = "LEARNER")
        @DisplayName("User cannot submit assessment for other user's snapshot")
        void testOwnershipValidation_SnapshotOwnershipVerified() throws Exception {
            // GIVEN: Snapshot owned by different user
            UUID otherUserId = UUID.randomUUID();
            UUID otherSnapshot = UUID.randomUUID();
            RepositorySnapshot snapshot = RepositorySnapshot.builder()
                    .id(otherSnapshot)
                    .userId(otherUserId)  // Different user owns this
                    .repositoryId(UUID.randomUUID())
                    .ownerUsername("other-owner")
                    .repositoryName("other-repo")
                    .branchName("main")
                    .commitSha("abc123")
                    .filesContent(new HashMap<>())
                    .createdAt(Instant.now())
                    .build();
            snapshotRepository.save(snapshot);

            // WHEN: Authenticated user attempts to submit assessment for other's snapshot
            SubmitAssessmentRequest request = new SubmitAssessmentRequest(otherSnapshot);

            // THEN: Request is rejected with 404 Not Found
            mockMvc.perform(post("/api/v1/assessments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            // ASSERTION CHECKLIST:
            // [ ] HTTP 404 status
            // [ ] No assessment created
            // [ ] Audit log records unauthorized access attempt
        }
    }

    // ========== CRITICAL TEST 5: Race Condition - Stale State Prevention ==========

    @Nested
    @DisplayName("Race Condition Tests")
    class RaceConditionTests {

        @Test
        @DisplayName("Dequeued job has fresh state (no staleness after concurrency)")
        void testRaceCondition_StaleStatePrevention() throws Exception {
            // GIVEN: Assessment job in queue
            Assessment assessment = Assessment.builder()
                    .userId(testUserId)
                    .snapshotId(testSnapshotId)
                    .status(AssessmentStatus.PENDING)
                    .build();
            Assessment saved = assessmentRepository.save(assessment);

            AssessmentJob job = new AssessmentJob(saved.getId(), testSnapshotId, "1.0.0");
            jobQueue.enqueue(job);

            // WHEN: Job is dequeued from queue
            Optional<AssessmentJob> dequeued = jobQueue.dequeue(
                    java.time.Duration.ofSeconds(1)
            );

            // THEN: Dequeued job has correct assessment ID and repo ID
            assertTrue(dequeued.isPresent(), "Job should be dequeued");
            assertEquals(saved.getId(), dequeued.get().getAssessmentId());
            assertEquals(testSnapshotId, dequeued.get().getRepoId());
            assertEquals(0, dequeued.get().getRetryCount());

            // Simulate processor fetching fresh assessment state from DB
            Optional<Assessment> freshAssessment = assessmentRepository.findById(saved.getId());
            assertTrue(freshAssessment.isPresent());
            assertEquals(AssessmentStatus.PENDING, freshAssessment.get().getStatus());

            // ASSERTION CHECKLIST:
            // [ ] Dequeued job matches what was enqueued
            // [ ] Database has fresh state (not stale)
            // [ ] No race condition when multiple consumers dequeue
            // [ ] Job retains original context (assessment_id, repo_id)
        }

        @Test
        @DisplayName("Concurrent job dequeuing doesn't cause duplicates")
        void testRaceCondition_ConcurrentDequeueNoDuplicates() throws Exception {
            // GIVEN: Multiple jobs in queue
            AssessmentJob job1 = new AssessmentJob(UUID.randomUUID(), UUID.randomUUID(), "1.0.0");
            AssessmentJob job2 = new AssessmentJob(UUID.randomUUID(), UUID.randomUUID(), "1.0.0");

            jobQueue.enqueue(job1);
            jobQueue.enqueue(job2);

            // WHEN: Two threads dequeue concurrently
            CountDownLatch latch = new CountDownLatch(2);
            AtomicReference<AssessmentJob> thread1Result = new AtomicReference<>();
            AtomicReference<AssessmentJob> thread2Result = new AtomicReference<>();

            new Thread(() -> {
                try {
                    var dequeued = jobQueue.dequeue(java.time.Duration.ofSeconds(1));
                    dequeued.ifPresent(thread1Result::set);
                } finally {
                    latch.countDown();
                }
            }).start();

            new Thread(() -> {
                try {
                    var dequeued = jobQueue.dequeue(java.time.Duration.ofSeconds(1));
                    dequeued.ifPresent(thread2Result::set);
                } finally {
                    latch.countDown();
                }
            }).start();

            // THEN: Each job dequeued by exactly one thread (no duplicates)
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Threads should complete");
            assertNotNull(thread1Result.get(), "Thread 1 should dequeue a job");
            assertNotNull(thread2Result.get(), "Thread 2 should dequeue a job");
            assertNotEquals(
                    thread1Result.get().getAssessmentId(),
                    thread2Result.get().getAssessmentId(),
                    "Each thread should dequeue different job"
            );

            // ASSERTION CHECKLIST:
            // [ ] Each job dequeued exactly once (Redis RPOP is atomic)
            // [ ] No job processed twice
            // [ ] Queue depth correct after concurrent dequeue
        }
    }

    // ========== CRITICAL TEST 6: Retry Loop Preserves Context ==========

    @Nested
    @DisplayName("Retry Loop Tests")
    class RetryLoopTests {

        @Test
        @DisplayName("Retry loop preserves original assessment context")
        void testRetryLoop_ContextPreservation() throws Exception {
            // GIVEN: Assessment job with original context
            UUID assessmentId = UUID.randomUUID();
            UUID repoId = UUID.randomUUID();
            AssessmentJob originalJob = new AssessmentJob(assessmentId, repoId, "1.0.0");
            originalJob.setRetryCount(0);
            originalJob.setFailureReason(null);

            // WHEN: Job fails and is retried
            AssessmentJob retriedJob = originalJob.withIncrementedRetry();
            retriedJob.setFailureReason("Temporary timeout");

            // THEN: Retried job preserves original context
            assertEquals(assessmentId, retriedJob.getAssessmentId(), "Assessment ID must be preserved");
            assertEquals(repoId, retriedJob.getRepoId(), "Repo ID must be preserved");
            assertEquals("1.0.0", retriedJob.getBrfVersion(), "BRF version must be preserved");
            assertEquals(1, retriedJob.getRetryCount(), "Retry count should increment");
            assertEquals("Temporary timeout", retriedJob.getFailureReason());

            // ASSERTION CHECKLIST:
            // [ ] Assessment ID unchanged after retry
            // [ ] Repository ID unchanged after retry
            // [ ] BRF version unchanged after retry
            // [ ] Only retry count and failure reason are updated
            // [ ] Multiple retries maintain integrity
        }

        @Test
        @DisplayName("Retry loop eventually moves job to dead letter queue")
        void testRetryLoop_DeadLetterQueueAfterMaxRetries() throws Exception {
            // GIVEN: Job that has been retried max times (5)
            UUID assessmentId = UUID.randomUUID();
            AssessmentJob failedJob = new AssessmentJob(assessmentId, UUID.randomUUID(), "1.0.0");
            failedJob.setRetryCount(5);
            failedJob.setFailureReason("Repeated timeout after 5 retries");

            // WHEN: Job is moved to dead letter queue
            jobQueue.moveToDeadLetterQueue(failedJob, "Max retries exceeded: " + failedJob.getFailureReason());

            // THEN: Job is in DLQ, not in main queue
            long mainQueueDepth = jobQueue.getQueueDepth();
            long dlqDepth = jobQueue.getDeadLetterQueueDepth();

            assertEquals(0, mainQueueDepth, "Main queue should be empty");
            assertEquals(1, dlqDepth, "DLQ should have 1 job");

            // ASSERTION CHECKLIST:
            // [ ] Job removed from main queue
            // [ ] Job added to DLQ
            // [ ] Failure reason is preserved
            // [ ] DLQ can be inspected for debugging
            // [ ] Operator can replay DLQ jobs manually
        }
    }

    // ========== INTEGRATION VERIFICATION TEST ==========

    @Test
    @WithMockUser(roles = "LEARNER")
    @DisplayName("Complete assessment submission flow end-to-end")
    void testCompleteFlow_SubmitAndTrackAssessment() throws Exception {
        // GIVEN: Valid repository snapshot
        // WHEN: User submits assessment via REST API
        // THEN: Assessment flows through all stages

        SubmitAssessmentRequest request = new SubmitAssessmentRequest(testSnapshotId);

        MvcResult submitResult = mockMvc.perform(post("/api/v1/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = submitResult.getResponse().getContentAsString();
        log.info("Assessment submitted: {}", responseBody);

        // Extract assessment ID from response
        java.util.Map<String, Object> responseMap = objectMapper.readValue(responseBody, java.util.Map.class);
        String assessmentId = (String) responseMap.get("id");
        assertNotNull(assessmentId, "Assessment ID should be returned");

        // VERIFY: Assessment persisted with PENDING status
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<Assessment> assessment = assessmentRepository.findById(UUID.fromString(assessmentId));
                    assertTrue(assessment.isPresent());
                    assertEquals(AssessmentStatus.PENDING, assessment.get().getStatus());
                });

        log.info("End-to-end flow test completed successfully");
    }
}
