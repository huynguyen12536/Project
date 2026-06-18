package com.learnhub.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepositoryDto(
    Long id,
    String name,
    String url,
    String description,
    String language,
    @JsonProperty("last_updated")
    String lastUpdated,
    @JsonProperty("size_kb")
    Integer sizeKb,
    @JsonProperty("is_private")
    Boolean isPrivate,
    @JsonProperty("is_fork")
    Boolean isFork
) {
    public GitHubRepositoryDto {
        // Sanitize description to prevent XSS
        if (description != null && description.length() > 500) {
            description = description.substring(0, 500);
        }
    }
}
