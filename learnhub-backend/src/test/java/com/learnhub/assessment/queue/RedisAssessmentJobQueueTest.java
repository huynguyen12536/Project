package com.learnhub.assessment.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisAssessmentJobQueue.
 *
 * Tests FIFO queue operations including enqueueing, dequeueing,
 * retrying, and moving jobs to dead-letter queue.
 */
@ExtendWith(MockitoExtension.class)
class RedisAssessmentJobQueueTest {

    @Mock
    private RedisTemplate<String, String> mockRedisTemplate;

    @Mock
    private ListOperations<String, String> mockListOps;

    private RedisAssessmentJobQueue jobQueue;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(mockRedisTemplate.opsForList()).thenReturn(mockListOps);
        objectMapper = new ObjectMapper();
        jobQueue = new RedisAssessmentJobQueue(mockRedisTemplate, objectMapper);
    }

    @Test
    void testEnqueue_ValidJob_SuccessfullyQueued() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "1.0.0");

        when(mockListOps.leftPush(eq("assessment_queue"), anyString()))
                .thenReturn(1L);

        // Act
        jobQueue.enqueue(job);

        // Assert
        verify(mockListOps).leftPush(eq("assessment_queue"), anyString());
    }

    @Test
    void testEnqueue_NullJob_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> jobQueue.enqueue(null),
                "Should throw exception for null job");
    }

    @Test
    void testEnqueue_JsonSerialization_CorrectFormat() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "1.0.0");

        when(mockListOps.leftPush(anyString(), anyString()))
                .thenReturn(1L);

        // Act
        jobQueue.enqueue(job);

        // Assert - verify correct JSON format was sent
        verify(mockListOps).leftPush(
                eq("assessment_queue"),
                argThat(json -> {
                    try {
                        AssessmentJob parsed = objectMapper.readValue(json, AssessmentJob.class);
                        return parsed.getAssessmentId().equals(assessmentId) &&
                               parsed.getRepoId().equals(repoId) &&
                               parsed.getBrfVersion().equals("1.0.0") &&
                               parsed.getRetryCount() == 0;
                    } catch (Exception e) {
                        return false;
                    }
                }));
    }

    @Test
    void testDequeue_JobAvailable_ReturnsJob() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "1.0.0");
        String jobJson = objectMapper.writeValueAsString(job);

        when(mockListOps.rightPop("assessment_queue", 1, TimeUnit.SECONDS))
                .thenReturn(jobJson);

        // Act
        Optional<AssessmentJob> result = jobQueue.dequeue(Duration.ofSeconds(1));

        // Assert
        assertTrue(result.isPresent(), "Should return Optional with job");
        AssessmentJob returned = result.get();
        assertEquals(assessmentId, returned.getAssessmentId());
        assertEquals(repoId, returned.getRepoId());
        assertEquals("1.0.0", returned.getBrfVersion());
    }

    @Test
    void testDequeue_Timeout_ReturnsEmpty() {
        // Arrange
        when(mockListOps.rightPop("assessment_queue", 1, TimeUnit.SECONDS))
                .thenReturn(null);

        // Act
        Optional<AssessmentJob> result = jobQueue.dequeue(Duration.ofSeconds(1));

        // Assert
        assertTrue(result.isEmpty(), "Should return empty Optional on timeout");
    }

    @Test
    void testDequeue_InvalidJson_ThrowsException() {
        // Arrange
        when(mockListOps.rightPop(anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn("invalid json");

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> jobQueue.dequeue(Duration.ofSeconds(1)),
                "Should throw exception for invalid JSON");
    }

    @Test
    void testRequeue_ValidJob_IncrementedRetryCount() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "1.0.0");
        job.setRetryCount(1);

        when(mockListOps.leftPush(anyString(), anyString()))
                .thenReturn(2L);

        // Act
        jobQueue.requeue(job);

        // Assert - verify retry count was incremented
        verify(mockListOps).leftPush(
                eq("assessment_queue"),
                argThat(json -> {
                    try {
                        AssessmentJob requeued = objectMapper.readValue(json, AssessmentJob.class);
                        return requeued.getRetryCount() == 2;  // Incremented from 1
                    } catch (Exception e) {
                        return false;
                    }
                }));
    }

    @Test
    void testRequeue_NullJob_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> jobQueue.requeue(null),
                "Should throw exception for null job");
    }

    @Test
    void testMoveToDeadLetterQueue_ValidJob_MovedToDlq() throws Exception {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        AssessmentJob job = new AssessmentJob(assessmentId, repoId, "1.0.0");
        String reason = "Max retries exceeded";

        when(mockListOps.leftPush(anyString(), anyString()))
                .thenReturn(1L);

        // Act
        jobQueue.moveToDeadLetterQueue(job, reason);

        // Assert - verify moved to DLQ with reason
        verify(mockListOps).leftPush(
                eq("assessment_dlq"),
                argThat(json -> {
                    try {
                        AssessmentJob dlqJob = objectMapper.readValue(json, AssessmentJob.class);
                        return dlqJob.getAssessmentId().equals(assessmentId) &&
                               reason.equals(dlqJob.getFailureReason());
                    } catch (Exception e) {
                        return false;
                    }
                }));
    }

    @Test
    void testMoveToDeadLetterQueue_NullJob_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> jobQueue.moveToDeadLetterQueue(null, "reason"),
                "Should throw exception for null job");
    }

    @Test
    void testMoveToDeadLetterQueue_NullReason_ThrowsException() {
        // Arrange
        AssessmentJob job = new AssessmentJob();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> jobQueue.moveToDeadLetterQueue(job, null),
                "Should throw exception for null reason");
    }

    @Test
    void testMoveToDeadLetterQueue_BlankReason_ThrowsException() {
        // Arrange
        AssessmentJob job = new AssessmentJob();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> jobQueue.moveToDeadLetterQueue(job, "   "),
                "Should throw exception for blank reason");
    }

    @Test
    void testGetQueueDepth_ReturnsDepth() {
        // Arrange
        when(mockListOps.size("assessment_queue"))
                .thenReturn(5L);

        // Act
        long depth = jobQueue.getQueueDepth();

        // Assert
        assertEquals(5L, depth);
        verify(mockListOps).size("assessment_queue");
    }

    @Test
    void testGetQueueDepth_NullResponse_ReturnsZero() {
        // Arrange
        when(mockListOps.size("assessment_queue"))
                .thenReturn(null);

        // Act
        long depth = jobQueue.getQueueDepth();

        // Assert
        assertEquals(0L, depth);
    }

    @Test
    void testGetDeadLetterQueueDepth_ReturnsDepth() {
        // Arrange
        when(mockListOps.size("assessment_dlq"))
                .thenReturn(3L);

        // Act
        long depth = jobQueue.getDeadLetterQueueDepth();

        // Assert
        assertEquals(3L, depth);
        verify(mockListOps).size("assessment_dlq");
    }

    @Test
    void testGetDeadLetterQueueDepth_NullResponse_ReturnsZero() {
        // Arrange
        when(mockListOps.size("assessment_dlq"))
                .thenReturn(null);

        // Act
        long depth = jobQueue.getDeadLetterQueueDepth();

        // Assert
        assertEquals(0L, depth);
    }

    @Test
    void testEnqueue_RedisFailure_ThrowsException() {
        // Arrange
        AssessmentJob job = new AssessmentJob();
        when(mockListOps.leftPush(anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis connection error"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> jobQueue.enqueue(job),
                "Should throw exception on Redis failure");
    }

    @Test
    void testDequeue_RedisFailure_ThrowsException() {
        // Arrange
        when(mockListOps.rightPop(anyString(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new RuntimeException("Redis connection error"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> jobQueue.dequeue(Duration.ofSeconds(1)),
                "Should throw exception on Redis failure");
    }
}
