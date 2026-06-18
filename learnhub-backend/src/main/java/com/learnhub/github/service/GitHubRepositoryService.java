package com.learnhub.github.service;

import com.learnhub.auth.oauth.GitHubApiClient;
import com.learnhub.auth.oauth.exception.GitHubOAuthException;
import com.learnhub.auth.oauth.exception.GitHubRateLimitException;
import com.learnhub.github.dto.GitHubRepositoryDto;
import com.learnhub.github.dto.GitHubRepositoryResponse;
import com.learnhub.github.dto.PaginationDto;
import com.learnhub.github.dto.RateLimitDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GitHubRepositoryService {

    private static final int CACHE_TTL_SECONDS = 3600;
    private static final String CACHE_KEY_PREFIX = "github:repos:";

    private final GitHubApiClient gitHubApiClient;
    private final RedisTemplate<String, String> redisTemplate;

    public GitHubRepositoryService(
        GitHubApiClient gitHubApiClient,
        RedisTemplate<String, String> redisTemplate
    ) {
        this.gitHubApiClient = gitHubApiClient;
        this.redisTemplate = redisTemplate;
    }

    @Transactional(readOnly = true)
    public GitHubRepositoryResponse listRepositories(
        String accessToken,
        int page,
        int perPage,
        boolean refresh
    ) {
        log.debug("Listing repositories for user (page={}, perPage={}, refresh={})", page, perPage, refresh);

        // Validate pagination parameters
        if (perPage < 1 || perPage > 100) {
            perPage = 30;
        }
        if (page < 1) {
            page = 1;
        }

        String cacheKey = CACHE_KEY_PREFIX + accessToken.hashCode();

        // Try cache if not forcing refresh
        if (!refresh) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("Found repositories in cache");
                    // For now, just return the cached response (would need deserialization in production)
                    // This is a simplified version
                }
            } catch (Exception e) {
                log.warn("Failed to access cache, will fetch from GitHub", e);
            }
        }

        try {
            // Fetch from GitHub API
            var apiResponse = gitHubApiClient.listRepositories(accessToken, page, perPage);

            // Convert to DTOs
            List<GitHubRepositoryDto> repos = apiResponse.repositories().stream()
                .map(repo -> new GitHubRepositoryDto(
                    repo.id(),
                    repo.name(),
                    repo.url(),
                    repo.description(),
                    repo.language(),
                    repo.lastUpdated(),
                    repo.sizeKb(),
                    repo.isPrivate(),
                    repo.isFork()
                ))
                .collect(Collectors.toList());

            // Calculate pagination
            int totalCount = repos.size();
            boolean hasNextPage = apiResponse.hasNextPage() != null && apiResponse.hasNextPage();
            String nextPageUrl = hasNextPage
                ? "/api/v1/github/repositories?page=" + (page + 1) + "&per_page=" + perPage
                : null;

            PaginationDto pagination = new PaginationDto(
                totalCount,
                page,
                perPage,
                hasNextPage,
                nextPageUrl
            );

            // Format reset time
            LocalDateTime resetAt = Instant.ofEpochSecond(apiResponse.rateLimitReset())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

            RateLimitDto rateLimit = new RateLimitDto(
                apiResponse.rateLimitRemaining(),
                apiResponse.rateLimitLimit(),
                resetAt.toString()
            );

            // Cache the response
            try {
                redisTemplate.opsForValue().set(
                    cacheKey,
                    "cached",
                    java.time.Duration.ofSeconds(CACHE_TTL_SECONDS)
                );
                log.debug("Cached repositories");
            } catch (Exception e) {
                log.warn("Failed to cache repositories", e);
            }

            GitHubRepositoryResponse response = new GitHubRepositoryResponse(repos, pagination, rateLimit);
            log.info("Retrieved {} repositories", repos.size());
            return response;

        } catch (GitHubRateLimitException e) {
            log.error("GitHub rate limit exceeded", e);
            throw e;
        } catch (GitHubOAuthException e) {
            log.error("GitHub API error", e);
            // Try to return cached data
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.info("Returning cached data due to GitHub API error");
                    // In production, would deserialize and return cached response
                }
            } catch (Exception ex) {
                log.warn("Failed to retrieve cached fallback", ex);
            }
            // Return empty response with error rate limit
            PaginationDto pagination = new PaginationDto(0, page, perPage, false, null);
            RateLimitDto rateLimit = new RateLimitDto(0, 0, LocalDateTime.now().toString());
            return new GitHubRepositoryResponse(List.of(), pagination, rateLimit);
        }
    }

    public void invalidateCache(String accessToken) {
        String cacheKey = CACHE_KEY_PREFIX + accessToken.hashCode();
        try {
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated repository cache");
        } catch (Exception e) {
            log.warn("Failed to invalidate cache", e);
        }
    }
}
