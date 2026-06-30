package com.learnhub.notification.queue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationJob {

    public enum Channel {
        EMAIL,
        SMS,
        PUSH
    }

    public enum Type {
        EMAIL_VERIFICATION_OTP
    }

    @JsonProperty("job_id")
    private UUID jobId;

    @JsonProperty("user_id")
    private UUID userId;

    private Channel channel;

    private Type type;

    private String recipient;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("otp")
    private String otp;

    @JsonProperty("retry_count")
    private int retryCount;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("failure_reason")
    private String failureReason;

    public NotificationJob withIncrementedRetry() {
        NotificationJob copy = NotificationJob.builder()
            .jobId(jobId)
            .userId(userId)
            .channel(channel)
            .type(type)
            .recipient(recipient)
            .firstName(firstName)
            .otp(otp)
            .retryCount(retryCount + 1)
            .createdAt(createdAt)
            .failureReason(failureReason)
            .build();
        return copy;
    }
}
