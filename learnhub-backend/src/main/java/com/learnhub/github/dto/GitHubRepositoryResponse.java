package com.learnhub.github.dto;

import java.util.List;

public record GitHubRepositoryResponse(
    List<GitHubRepositoryDto> repositories,
    PaginationDto pagination,
    RateLimitDto rateLimit
) {}
