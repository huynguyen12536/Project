package com.learnhub.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for repository snapshots.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotResponse {

    @JsonProperty("snapshotId")
    private UUID snapshotId;

    @JsonProperty("githubRepoId")
    private Long githubRepoId;

    @JsonProperty("branch")
    private String branch;

    @JsonProperty("commitSha")
    private String commitSha;

    @JsonProperty("filesCount")
    private Integer filesCount;

    @JsonProperty("totalSizeKb")
    private Long totalSizeKb;

    @JsonProperty("languages")
    private Map<String, Integer> languages;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
