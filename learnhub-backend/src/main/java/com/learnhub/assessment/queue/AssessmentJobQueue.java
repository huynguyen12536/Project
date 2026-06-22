package com.learnhub.assessment.queue;

import java.time.Duration;
import java.util.Optional;

/**
 * Interface for assessment job queue operations.
 *
 * Provides abstract operations for enqueueing assessment jobs, dequeuing
 * for processing, handling retries, and managing dead-letter queue (DLQ).
 *
 * Implementation Details:
 * - Thread-safe: Multiple consumers can safely read from queue
 * - Atomic operations: Redis transactions ensure data consistency
 * - Connection pooling: Handled by Spring Data Redis
 * - Serialization: JSON format for easy debugging and transport
 */
public interface AssessmentJobQueue {

    /**
     * Enqueue an assessment job for processing.
     *
     * @param job the job to enqueue
     * @throws IllegalArgumentException if job is null
     * @throws RuntimeException if Redis operation fails
     */
    void enqueue(AssessmentJob job);

    /**
     * Dequeue a job from the queue with blocking behavior.
     *
     * Blocks until a job is available or timeout expires.
     * Returns empty Optional if timeout occurs.
     *
     * @param timeout duration to wait for job availability
     * @return Optional containing job if available, empty if timeout
     * @throws RuntimeException if Redis operation fails
     */
    Optional<AssessmentJob> dequeue(Duration timeout);

    /**
     * Requeue a job for retry (e.g., after transient failure).
     *
     * Adds job back to queue for another attempt.
     * Preserves original job data including retry count.
     *
     * @param job the job to requeue
     * @throws IllegalArgumentException if job is null
     * @throws RuntimeException if Redis operation fails
     */
    void requeue(AssessmentJob job);

    /**
     * Move a job to dead-letter queue (DLQ) for manual review.
     *
     * Called when job processing fails after all retries exhausted.
     * DLQ jobs should be periodically reviewed and either fixed or discarded.
     *
     * @param job the failed job
     * @param reason descriptive reason for moving to DLQ
     * @throws IllegalArgumentException if job or reason is null
     * @throws RuntimeException if Redis operation fails
     */
    void moveToDeadLetterQueue(AssessmentJob job, String reason);

    /**
     * Get current queue depth (number of pending jobs).
     *
     * @return number of jobs in queue
     * @throws RuntimeException if Redis operation fails
     */
    long getQueueDepth();

    /**
     * Get current DLQ depth (number of failed jobs awaiting review).
     *
     * @return number of jobs in DLQ
     * @throws RuntimeException if Redis operation fails
     */
    long getDeadLetterQueueDepth();
}
