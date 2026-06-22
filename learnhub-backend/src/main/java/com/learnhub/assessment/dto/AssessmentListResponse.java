package com.learnhub.assessment.dto;

import com.learnhub.assessment.entity.Assessment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for a list of assessments.
 * Provides pagination-friendly structure with total count.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentListResponse {

    private List<AssessmentResponse> assessments;
    private int total;

    /**
     * Convert a list of Assessment entities to a response DTO.
     */
    public static AssessmentListResponse fromEntities(List<Assessment> assessments) {
        List<AssessmentResponse> responses = assessments.stream()
            .map(AssessmentResponse::fromEntity)
            .collect(Collectors.toList());
        return new AssessmentListResponse(responses, responses.size());
    }
}
