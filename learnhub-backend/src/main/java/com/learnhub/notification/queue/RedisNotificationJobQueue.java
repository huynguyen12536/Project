package com.learnhub.notification.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisNotificationJobQueue implements NotificationJobQueue {

    private static final String QUEUE_NAME = "notification_queue";
    private static final String RETRY_QUEUE_NAME = "notification_retry_queue";
    private static final String DLQ_NAME = "notification_dlq";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisNotificationJobQueue(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enqueue(NotificationJob job) {
        if (job == null) {
            throw new IllegalArgumentException("Notification job must not be null");
        }

        try {
            String jobJson = objectMapper.writeValueAsString(job);
            Long position = redisTemplate.opsForList().leftPush(QUEUE_NAME, jobJson);
            log.info("Notification job enqueued: jobId={}, channel={}, type={}, position={}",
                job.getJobId(), job.getChannel(), job.getType(), position);
        } catch (Exception e) {
            log.error("Failed to enqueue notification job: jobId={}", job.getJobId(), e);
            throw new RuntimeException("Failed to enqueue notification job", e);
        }
    }

    @Override
    public Optional<NotificationJob> dequeue(Duration timeout) {
        promoteDueRetries();

        try {
            String jobJson = redisTemplate.opsForList()
                .rightPop(QUEUE_NAME, timeout.getSeconds(), TimeUnit.SECONDS);
            if (jobJson == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(jobJson, NotificationJob.class));
        } catch (Exception e) {
            log.error("Failed to dequeue notification job", e);
            throw new RuntimeException("Failed to dequeue notification job", e);
        }
    }

    @Override
    public void requeue(NotificationJob job, Duration delay) {
        if (job == null) {
            throw new IllegalArgumentException("Notification job must not be null");
        }

        try {
            NotificationJob retryJob = job.withIncrementedRetry();
            String jobJson = objectMapper.writeValueAsString(retryJob);
            double runAtEpochMillis = Instant.now().plus(delay).toEpochMilli();
            redisTemplate.opsForZSet().add(RETRY_QUEUE_NAME, jobJson, runAtEpochMillis);
            log.warn("Notification job scheduled for retry: jobId={}, retryCount={}, delaySeconds={}",
                retryJob.getJobId(), retryJob.getRetryCount(), delay.toSeconds());
        } catch (Exception e) {
            log.error("Failed to requeue notification job: jobId={}", job.getJobId(), e);
            throw new RuntimeException("Failed to requeue notification job", e);
        }
    }

    @Override
    public void moveToDeadLetterQueue(NotificationJob job, String reason) {
        if (job == null) {
            throw new IllegalArgumentException("Notification job must not be null");
        }

        try {
            job.setFailureReason(reason);
            String jobJson = objectMapper.writeValueAsString(job);
            redisTemplate.opsForList().leftPush(DLQ_NAME, jobJson);
            log.error("Notification job moved to DLQ: jobId={}, channel={}, type={}, reason={}",
                job.getJobId(), job.getChannel(), job.getType(), reason);
        } catch (Exception e) {
            log.error("Failed to move notification job to DLQ: jobId={}", job.getJobId(), e);
            throw new RuntimeException("Failed to move notification job to DLQ", e);
        }
    }

    @Override
    public long getQueueDepth() {
        Long depth = redisTemplate.opsForList().size(QUEUE_NAME);
        return depth == null ? 0 : depth;
    }

    @Override
    public long getDeadLetterQueueDepth() {
        Long depth = redisTemplate.opsForList().size(DLQ_NAME);
        return depth == null ? 0 : depth;
    }

    private void promoteDueRetries() {
        long now = Instant.now().toEpochMilli();
        Set<String> dueJobs = redisTemplate.opsForZSet().rangeByScore(RETRY_QUEUE_NAME, 0, now, 0, 25);
        if (dueJobs == null || dueJobs.isEmpty()) {
            return;
        }

        for (String jobJson : dueJobs) {
            Long removed = redisTemplate.opsForZSet().remove(RETRY_QUEUE_NAME, jobJson);
            if (removed != null && removed > 0) {
                redisTemplate.opsForList().leftPush(QUEUE_NAME, jobJson);
            }
        }
    }
}
