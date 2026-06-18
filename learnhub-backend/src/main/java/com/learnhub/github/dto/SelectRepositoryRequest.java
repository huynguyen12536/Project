package com.learnhub.github.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for selecting a GitHub repository.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectRepositoryRequest {

    @NotNull(message = "GitHub repository ID must not be null")
    @Positive(message = "GitHub repository ID must be positive")
    private Long githubRepoId;
}
