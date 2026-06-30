package com.learnhub.notification.worker;

import com.learnhub.notification.queue.NotificationJob;
import com.learnhub.notification.queue.NotificationJobQueue;
import com.learnhub.user.model.User;
import com.learnhub.user.service.EmailNotificationService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public class NotificationWorker {

    private final NotificationJobQueue notificationJobQueue;
    private final EmailNotificationService emailNotificationService;

    @Value("${app.notification.poll-timeout-seconds:1}")
    private int pollTimeoutSeconds;

    @Value("${app.notification.max-retries:4}")
    private int maxRetries;

    public NotificationWorker(
        NotificationJobQueue notificationJobQueue,
        EmailNotificationService emailNotificationService
    ) {
        this.notificationJobQueue = notificationJobQueue;
        this.emailNotificationService = emailNotificationService;
    }

    @PostConstruct
    public void start() {
        Thread worker = new Thread(this::pollAndProcess, "NotificationWorker-Thread");
        worker.setDaemon(true);
        worker.start();
        log.info("NotificationWorker started");
    }

    private void pollAndProcess() {
        Duration timeout = Duration.ofSeconds(pollTimeoutSeconds);

        while (true) {
            try {
                Optional<NotificationJob> job = notificationJobQueue.dequeue(timeout);
                job.ifPresent(this::processWithRetry);
            } catch (Exception e) {
                log.error("Unexpected error in notification worker loop", e);
            }
        }
    }

    private void processWithRetry(NotificationJob job) {
        try {
            process(job);
            log.info("Notification job processed: jobId={}, channel={}, type={}",
                job.getJobId(), job.getChannel(), job.getType());
        } catch (Exception e) {
            int nextRetryCount = job.getRetryCount() + 1;
            if (nextRetryCount >= maxRetries) {
                notificationJobQueue.moveToDeadLetterQueue(job, e.getMessage());
                return;
            }

            Duration delay = retryDelay(nextRetryCount);
            log.warn("Notification job failed, scheduling retry: jobId={}, attempt={}/{}, delaySeconds={}, error={}",
                job.getJobId(), nextRetryCount, maxRetries, delay.toSeconds(), e.getMessage());
            notificationJobQueue.requeue(job, delay);
        }
    }

    private void process(NotificationJob job) {
        if (job.getChannel() != NotificationJob.Channel.EMAIL) {
            throw new UnsupportedOperationException("Unsupported notification channel: " + job.getChannel());
        }

        if (job.getType() == NotificationJob.Type.EMAIL_VERIFICATION_OTP) {
            User user = new User();
            user.setId(job.getUserId());
            user.setEmail(job.getRecipient());
            user.setFirstName(job.getFirstName());
            emailNotificationService.sendVerificationOtpEmail(user, job.getOtp());
            return;
        }

        throw new UnsupportedOperationException("Unsupported notification type: " + job.getType());
    }

    private Duration retryDelay(int retryAttempt) {
        return switch (retryAttempt) {
            case 1 -> Duration.ofSeconds(30);
            case 2 -> Duration.ofMinutes(2);
            default -> Duration.ofMinutes(10);
        };
    }
}
