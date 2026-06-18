package com.learnhub.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for repository selection.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectRepositoryResponse {

    @JsonProperty("selectionId")
    private UUID selectionId;

    @JsonProperty("githubRepoId")
    private Long githubRepoId;

    @JsonProperty("selectedAt")
    private LocalDateTime selectedAt;
}
