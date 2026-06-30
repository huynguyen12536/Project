package com.learnhub.notification.service;

import com.learnhub.notification.queue.NotificationJob;
import com.learnhub.notification.queue.NotificationJobQueue;
import com.learnhub.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationJobQueue notificationJobQueue;

    public void queueVerificationOtpEmail(User user, String otp) {
        NotificationJob job = NotificationJob.builder()
            .jobId(UUID.randomUUID())
            .userId(user.getId())
            .channel(NotificationJob.Channel.EMAIL)
            .type(NotificationJob.Type.EMAIL_VERIFICATION_OTP)
            .recipient(user.getEmail())
            .firstName(user.getFirstName())
            .otp(otp)
            .retryCount(0)
            .createdAt(Instant.now())
            .build();

        enqueueAfterCommit(job);
    }

    private void enqueueAfterCommit(NotificationJob job) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeEnqueue(job);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeEnqueue(job);
            }
        });
    }

    private void safeEnqueue(NotificationJob job) {
        try {
            notificationJobQueue.enqueue(job);
        } catch (Exception e) {
            log.error("Failed to enqueue notification job after user transaction committed: jobId={}, userId={}",
                job.getJobId(), job.getUserId(), e);
        }
    }
}
