package com.learnhub.assessment.dto;

import com.learnhub.assessment.entity.Assessment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Response DTO for a single assessment.
 * Returns assessment details including status and timestamps.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResponse {

    private UUID id;
    private UUID userId;
    private UUID snapshotId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    /**
     * Convert an Assessment entity to a response DTO.
     */
    public static AssessmentResponse fromEntity(Assessment assessment) {
        return new AssessmentResponse(
            assessment.getId(),
            assessment.getUserId(),
            assessment.getSnapshotId(),
            assessment.getStatus().name(),
            convertToLocalDateTime(assessment.getCreatedAt()),
            convertToLocalDateTime(assessment.getCompletedAt())
        );
    }

    private static LocalDateTime convertToLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }
}
