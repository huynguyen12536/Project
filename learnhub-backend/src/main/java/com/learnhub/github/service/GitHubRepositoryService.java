package com.learnhub.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
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
    private final ObjectMapper objectMapper;

    public GitHubRepositoryService(
        GitHubApiClient gitHubApiClient,
        RedisTemplate<String, String> redisTemplate,
        ObjectMapper objectMapper
    ) {
        this.gitHubApiClient = gitHubApiClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public GitHubRepositoryResponse listRepositories(
        String accessToken,
        int page,
        int perPage,
        boolean refresh
    ) {
        log.debug("Listing repositories for user (page={}, perPage={}, refresh={})", page, perPage, refresh);

        // Validate access token
        if (accessToken == null || accessToken.isBlank()) {
            log.error("Access token is null or blank");
            throw new IllegalArgumentException("GitHub access token is required and must not be blank");
        }

        // Validate pagination parameters
        if (perPage < 1 || perPage > 100) {
            perPage = 30;
        }
        if (page < 1) {
            page = 1;
        }

        // Use SHA-256 hash instead of hashCode() to prevent collisions
        // hashCode() can collide; different tokens could hash to same value
        String cacheKey;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] tokenHash = digest.digest(accessToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String tokenHex = HexFormat.of().formatHex(tokenHash).substring(0, 16);
            cacheKey = CACHE_KEY_PREFIX + tokenHex;
        } catch (Exception e) {
            log.warn("Failed to hash token for cache key, falling back to page-based caching", e);
            cacheKey = CACHE_KEY_PREFIX + "page:" + page;
        }

        // Try cache if not forcing refresh
        if (!refresh) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("Cache hit for repositories");
                    try {
                        GitHubRepositoryResponse cachedResponse = objectMapper.readValue(
                            cached,
                            GitHubRepositoryResponse.class
                        );
                        return cachedResponse;
                    } catch (Exception e) {
                        log.warn("Failed to deserialize cached response, fetching fresh", e);
                        // Continue to fetch from GitHub
                    }
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
            // Note: GitHub API does not provide total repository count for authenticated users.
            // The 'totalCount' field is set to -1 to indicate that the total number of repositories
            // is unknown. Clients should use 'has_next_page' to determine if more pages exist.
            int totalCount = -1;  // Unknown total (GitHub API limitation)
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

            GitHubRepositoryResponse response = new GitHubRepositoryResponse(repos, pagination, rateLimit);

            // Cache the response with proper serialization
            try {
                String serialized = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(
                    cacheKey,
                    serialized,
                    java.time.Duration.ofSeconds(CACHE_TTL_SECONDS)
                );
                log.debug("Cached {} repositories for {} seconds", repos.size(), CACHE_TTL_SECONDS);
            } catch (Exception e) {
                log.warn("Failed to cache repositories", e);
            }

            log.info("Retrieved {} repositories", repos.size());
            return response;

        } catch (GitHubRateLimitException e) {
            log.error("GitHub rate limit exceeded", e);
            throw e;
        } catch (GitHubOAuthException e) {
            log.error("GitHub API error", e);
            // Try to return cached data as fallback
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.info("Returning cached data due to GitHub API error");
                    try {
                        GitHubRepositoryResponse cachedResponse = objectMapper.readValue(
                            cached,
                            GitHubRepositoryResponse.class
                        );
                        return cachedResponse;
                    } catch (Exception deserializeEx) {
                        log.warn("Failed to deserialize cached fallback", deserializeEx);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to retrieve cached fallback", ex);
            }
            // Return empty response with error rate limit
            PaginationDto pagination = new PaginationDto(-1, page, perPage, false, null);
            RateLimitDto rateLimit = new RateLimitDto(0, 0, LocalDateTime.now().toString());
            return new GitHubRepositoryResponse(List.of(), pagination, rateLimit);
        }
    }

    public void invalidateCache(String accessToken) {
        try {
            // Use same SHA-256 hash approach as listRepositories for consistency
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] tokenHash = digest.digest(accessToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String tokenHex = HexFormat.of().formatHex(tokenHash).substring(0, 16);
            String cacheKey = CACHE_KEY_PREFIX + tokenHex;

            redisTemplate.delete(cacheKey);
            log.debug("Invalidated repository cache");
        } catch (Exception e) {
            log.warn("Failed to invalidate cache", e);
        }
    }
}
