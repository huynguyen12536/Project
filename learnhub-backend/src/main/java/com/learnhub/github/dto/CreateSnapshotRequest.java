package com.learnhub.github.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating a repository snapshot.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSnapshotRequest {

    @NotNull(message = "GitHub repository ID must not be null")
    @Positive(message = "GitHub repository ID must be positive")
    private Long githubRepoId;

    @Size(max = 255, message = "Branch name must not exceed 255 characters")
    private String branch;

    @Size(max = 40, message = "Commit SHA must not exceed 40 characters")
    private String commitSha;

    @NotNull(message = "Files count must not be null")
    @PositiveOrZero(message = "Files count must be non-negative")
    private Integer filesCount;

    @NotNull(message = "Total size must not be null")
    @PositiveOrZero(message = "Total size must be non-negative")
    private Long totalSizeKb;

    private Map<String, Integer> languages;
}
