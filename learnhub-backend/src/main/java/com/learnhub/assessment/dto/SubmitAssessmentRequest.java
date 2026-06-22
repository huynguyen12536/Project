package com.learnhub.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for submitting a new assessment.
 * Requires the ID of the repository snapshot to be assessed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAssessmentRequest {

    @NotNull(message = "Snapshot ID must not be null")
    private UUID snapshotId;
}
