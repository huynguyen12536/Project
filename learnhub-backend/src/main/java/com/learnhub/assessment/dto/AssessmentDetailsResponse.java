package com.learnhub.assessment.dto;

import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive assessment response DTO including results.
 * Returned when fetching completed assessments with full details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentDetailsResponse {

    // Assessment metadata
    private UUID id;
    private UUID userId;
    private UUID snapshotId;
    private String status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;

    // Assessment results (if COMPLETED)
    private AssessmentResultDetails result;

    /**
     * Nested DTO for detailed assessment results.
     * Returned alongside Assessment data when assessment is complete.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssessmentResultDetails {
        private UUID resultId;
        private String overallLevel;  // NOT_DEMONSTRATED, EMERGING, PROFICIENT, ADVANCED
        private List<String> allGaps;  // All identified gaps
        private List<String> nextSteps;  // Recommendations
        private Double overallConfidence;  // 0.0 - 1.0
        private Map<String, Object> detections;  // Detailed detector results

        /**
         * Convert AssessmentResult entity to DTO.
         */
        public static AssessmentResultDetails fromEntity(AssessmentResult entity) {
            if (entity == null) {
                return null;
            }

            return AssessmentResultDetails.builder()
                    .resultId(entity.getId())
                    .overallLevel(entity.getOverallLevel())
                    .allGaps(entity.getAllGaps())
                    .nextSteps(entity.getNextSteps())
                    .overallConfidence(entity.getOverallConfidence())
                    .build();
        }
    }

    /**
     * Convert Assessment and optional AssessmentResult to response DTO.
     */
    public static AssessmentDetailsResponse fromEntities(
            Assessment assessment,
            AssessmentResult result) {
        return AssessmentDetailsResponse.builder()
                .id(assessment.getId())
                .userId(assessment.getUserId())
                .snapshotId(assessment.getSnapshotId())
                .status(assessment.getStatus().name())
                .createdAt(assessment.getCreatedAt())
                .startedAt(assessment.getStartedAt())
                .completedAt(assessment.getCompletedAt())
                .errorMessage(assessment.getErrorMessage())
                .result(AssessmentResultDetails.fromEntity(result))
                .build();
    }
}
