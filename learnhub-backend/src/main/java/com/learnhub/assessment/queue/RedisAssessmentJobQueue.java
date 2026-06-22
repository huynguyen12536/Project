package com.learnhub.assessment.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of AssessmentJobQueue.
 *
 * Uses Redis lists for FIFO queue behavior with BRPOP for blocking reads.
 * Handles serialization/deserialization using Jackson ObjectMapper.
 *
 * Queue Names:
 * - assessment_queue: Primary queue for pending jobs
 * - assessment_dlq: Dead Letter Queue for failed jobs awaiting review
 *
 * Thread Safety:
 * - Redis operations are atomic at the protocol level
 * - Multiple consumers can safely pop from queue simultaneously
 * - No internal synchronization needed (Redis handles locking)
 */
@Component
@Slf4j
public class RedisAssessmentJobQueue implements AssessmentJobQueue {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String QUEUE_NAME = "assessment_queue";
    private static final String DLQ_NAME = "assessment_dlq";

    /**
     * Create Redis assessment job queue.
     *
     * @param redisTemplate Spring Data Redis template for operations
     * @param objectMapper Jackson mapper for JSON serialization
     */
    public RedisAssessmentJobQueue(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        log.info("RedisAssessmentJobQueue initialized");
    }

    /**
     * Enqueue an assessment job.
     *
     * Serializes job to JSON and pushes to Redis list.
     * Uses LPUSH so jobs are added to front of queue (FIFO when paired with RPOP).
     *
     * @param job the job to enqueue
     * @throws IllegalArgumentException if job is null
     * @throws RuntimeException if serialization or Redis operation fails
     */
    @Override
    public void enqueue(AssessmentJob job) {
        if (job == null) {
            throw new IllegalArgumentException("Assessment job must not be null");
        }

        try {
            String jobJson = objectMapper.writeValueAsString(job);
            Long position = redisTemplate.opsForList()
                    .leftPush(QUEUE_NAME, jobJson);
            log.info("Job enqueued: assessmentId={}, position={}",
                    job.getAssessmentId(), position);
        } catch (Exception e) {
            log.error("Failed to enqueue job: {}", job.getAssessmentId(), e);
            throw new RuntimeException("Failed to enqueue assessment job", e);
        }
    }

    /**
     * Dequeue a job with blocking behavior.
     *
     * Uses BRPOP (blocking right pop) to wait for job availability.
     * This is efficient compared to polling: Redis blocks the connection
     * until a job is available or timeout expires.
     *
     * @param timeout duration to wait for job
     * @return Optional with job if available, empty if timeout
     * @throws RuntimeException if deserialization or Redis operation fails
     */
    @Override
    public Optional<AssessmentJob> dequeue(Duration timeout) {
        try {
            String jobJson = redisTemplate.opsForList()
                    .rightPop(QUEUE_NAME, timeout.getSeconds(), TimeUnit.SECONDS);

            if (jobJson == null) {
                // Timeout occurred, no job available
                return Optional.empty();
            }

            AssessmentJob job = objectMapper.readValue(jobJson, AssessmentJob.class);
            log.debug("Job dequeued: assessmentId={}, retryCount={}",
                    job.getAssessmentId(), job.getRetryCount());
            return Optional.of(job);

        } catch (Exception e) {
            log.error("Failed to dequeue job", e);
            throw new RuntimeException("Failed to dequeue assessment job", e);
        }
    }

    /**
     * Requeue a job for retry.
     *
     * Adds job back to the queue with incremented retry count.
     * Used when transient failures occur and retry is appropriate.
     *
     * @param job the job to requeue
     * @throws IllegalArgumentException if job is null
     * @throws RuntimeException if serialization or Redis operation fails
     */
    @Override
    public void requeue(AssessmentJob job) {
        if (job == null) {
            throw new IllegalArgumentException("Assessment job must not be null");
        }

        try {
            AssessmentJob retryJob = job.withIncrementedRetry();
            String jobJson = objectMapper.writeValueAsString(retryJob);
            Long position = redisTemplate.opsForList()
                    .leftPush(QUEUE_NAME, jobJson);
            log.info("Job requeued: assessmentId={}, retryCount={}, position={}",
                    job.getAssessmentId(), retryJob.getRetryCount(), position);
        } catch (Exception e) {
            log.error("Failed to requeue job: {}", job.getAssessmentId(), e);
            throw new RuntimeException("Failed to requeue assessment job", e);
        }
    }

    /**
     * Move job to dead-letter queue.
     *
     * Called when job processing fails after all retries exhausted.
     * Adds descriptive reason to help with manual review.
     *
     * @param job the failed job
     * @param reason descriptive reason for failure
     * @throws IllegalArgumentException if job or reason is null
     * @throws RuntimeException if serialization or Redis operation fails
     */
    @Override
    public void moveToDeadLetterQueue(AssessmentJob job, String reason) {
        if (job == null) {
            throw new IllegalArgumentException("Assessment job must not be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Failure reason must not be null or blank");
        }

        try {
            job.setFailureReason(reason);
            String jobJson = objectMapper.writeValueAsString(job);
            Long position = redisTemplate.opsForList()
                    .leftPush(DLQ_NAME, jobJson);
            log.warn("Job moved to DLQ: assessmentId={}, reason={}, position={}",
                    job.getAssessmentId(), reason, position);
        } catch (Exception e) {
            log.error("Failed to move job to DLQ: {}", job.getAssessmentId(), e);
            throw new RuntimeException("Failed to move assessment job to DLQ", e);
        }
    }

    /**
     * Get current queue depth.
     *
     * @return number of pending jobs in queue
     * @throws RuntimeException if Redis operation fails
     */
    @Override
    public long getQueueDepth() {
        try {
            Long depth = redisTemplate.opsForList().size(QUEUE_NAME);
            return depth != null ? depth : 0;
        } catch (Exception e) {
            log.error("Failed to get queue depth", e);
            throw new RuntimeException("Failed to get queue depth", e);
        }
    }

    /**
     * Get current dead-letter queue depth.
     *
     * @return number of failed jobs in DLQ
     * @throws RuntimeException if Redis operation fails
     */
    @Override
    public long getDeadLetterQueueDepth() {
        try {
            Long depth = redisTemplate.opsForList().size(DLQ_NAME);
            return depth != null ? depth : 0;
        } catch (Exception e) {
            log.error("Failed to get DLQ depth", e);
            throw new RuntimeException("Failed to get DLQ depth", e);
        }
    }
}
