package com.learnhub.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaginationDto(
    @JsonProperty("total_count")
    Integer totalCount,
    @JsonProperty("current_page")
    Integer currentPage,
    @JsonProperty("per_page")
    Integer perPage,
    @JsonProperty("has_next_page")
    Boolean hasNextPage,
    @JsonProperty("next_page_url")
    String nextPageUrl
) {}
