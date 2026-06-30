package com.learnhub.notification.queue;

import java.time.Duration;
import java.util.Optional;

public interface NotificationJobQueue {

    void enqueue(NotificationJob job);

    Optional<NotificationJob> dequeue(Duration timeout);

    void requeue(NotificationJob job, Duration delay);

    void moveToDeadLetterQueue(NotificationJob job, String reason);

    long getQueueDepth();

    long getDeadLetterQueueDepth();
}
