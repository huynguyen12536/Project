package com.learnhub.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RateLimitDto(
    Integer remaining,
    Integer limit,
    @JsonProperty("reset_at")
    String resetAt
) {}
