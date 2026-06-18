package com.learnhub.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for GitHub OAuth and API calls.
 * Handles token exchange, user profile retrieval, and token refresh.
 *
 * See: https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps
 */
@Component
@Slf4j
public class GitHubApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GitHubOAuthProperties properties;

    public GitHubApiClient(RestTemplate restTemplate, ObjectMapper objectMapper, GitHubOAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Exchange authorization code for access token.
     *
     * @param code The authorization code from GitHub callback
     * @return Token response with access_token, refresh_token, expires_in
     * @throws GitHubOAuthException if exchange fails
     */
    public GitHubTokenResponse exchangeCodeForToken(String code) {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", properties.getClientId());
            body.add("client_secret", properties.getClientSecret());
            body.add("code", code);
            body.add("redirect_uri", properties.getRedirectUri());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            log.debug("Exchanging authorization code for access token");
            ResponseEntity<String> response = restTemplate.exchange(
                properties.getTokenUrl(),
                HttpMethod.POST,
                request,
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("GitHub token exchange failed: {} {}", response.getStatusCode(), response.getBody());
                throw new InvalidAuthorizationCodeException("Failed to exchange code for token");
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                log.error("GitHub token exchange returned empty response");
                throw new GitHubAPIException("GitHub token endpoint returned empty response");
            }

            JsonNode responseNode = objectMapper.readTree(responseBody);

            // Check for errors in response
            if (responseNode.has("error")) {
                String error = responseNode.get("error").asText();
                String errorDescription = responseNode.has("error_description")
                    ? responseNode.get("error_description").asText()
                    : "Unknown error";
                log.warn("GitHub API error: {} - {}", error, errorDescription);
                throw new InvalidAuthorizationCodeException("GitHub error: " + errorDescription);
            }

            String accessToken = responseNode.get("access_token").asText();
            String refreshToken = responseNode.has("refresh_token")
                ? responseNode.get("refresh_token").asText()
                : null;
            int expiresIn = responseNode.has("expires_in")
                ? responseNode.get("expires_in").asInt()
                : 3600;

            log.info("Successfully exchanged authorization code for access token (expires in {} seconds)", expiresIn);
            return new GitHubTokenResponse(accessToken, refreshToken, expiresIn);

        } catch (InvalidAuthorizationCodeException e) {
            throw e;
        } catch (RestClientException | RuntimeException e) {
            log.error("Failed to exchange code for token", e);
            throw new GitHubAPIException("Failed to communicate with GitHub OAuth endpoint", e);
        }
    }

    /**
     * Refresh an expired access token using the refresh token.
     *
     * @param refreshToken The refresh token
     * @return New token response
     * @throws GitHubOAuthException if refresh fails
     */
    public GitHubTokenResponse refreshAccessToken(String refreshToken) {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", properties.getClientId());
            body.add("client_secret", properties.getClientSecret());
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", refreshToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            log.debug("Refreshing GitHub access token");
            ResponseEntity<String> response = restTemplate.exchange(
                properties.getTokenUrl(),
                HttpMethod.POST,
                request,
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("GitHub token refresh failed: {}", response.getStatusCode());
                throw new TokenRefreshException("Failed to refresh token");
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                log.error("GitHub token refresh returned empty response");
                throw new GitHubAPIException("GitHub token endpoint returned empty response");
            }

            JsonNode responseNode = objectMapper.readTree(responseBody);

            if (responseNode.has("error")) {
                String error = responseNode.get("error").asText();
                log.warn("GitHub token refresh error: {}", error);
                throw new TokenRefreshException("GitHub token refresh failed: " + error);
            }

            String newAccessToken = responseNode.get("access_token").asText();
            String newRefreshToken = responseNode.has("refresh_token")
                ? responseNode.get("refresh_token").asText()
                : null;
            int expiresIn = responseNode.has("expires_in")
                ? responseNode.get("expires_in").asInt()
                : 3600;

            log.info("Successfully refreshed GitHub access token");
            return new GitHubTokenResponse(newAccessToken, newRefreshToken, expiresIn);

        } catch (TokenRefreshException e) {
            throw e;
        } catch (RestClientException | RuntimeException e) {
            log.error("Failed to refresh GitHub token", e);
            throw new GitHubAPIException("Failed to refresh GitHub token", e);
        }
    }

    /**
     * Get GitHub user profile using access token.
     *
     * @param accessToken The access token
     * @return User profile with login, id, email, avatar_url
     * @throws GitHubOAuthException if profile retrieval fails
     */
    public GitHubUserProfile getUserProfile(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<String> request = new HttpEntity<>(headers);

            log.debug("Fetching GitHub user profile");
            ResponseEntity<String> response = restTemplate.exchange(
                properties.getUserUrl(),
                HttpMethod.GET,
                request,
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("GitHub user profile retrieval failed: {}", response.getStatusCode());
                throw new GitHubAPIException("Failed to fetch user profile");
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                log.error("GitHub user profile endpoint returned empty response");
                throw new GitHubAPIException("GitHub user profile endpoint returned empty response");
            }

            JsonNode userNode = objectMapper.readTree(responseBody);

            String login = userNode.get("login").asText();
            Long id = userNode.get("id").asLong();
            String email = userNode.has("email") && !userNode.get("email").isNull()
                ? userNode.get("email").asText()
                : null;
            String avatarUrl = userNode.has("avatar_url")
                ? userNode.get("avatar_url").asText()
                : null;

            log.info("Retrieved GitHub user profile: login={}, id={}", login, id);
            return new GitHubUserProfile(id, login, email, avatarUrl);

        } catch (RestClientException | RuntimeException e) {
            log.error("Failed to fetch GitHub user profile", e);
            throw new GitHubAPIException("Failed to fetch GitHub user profile", e);
        }
    }

    /**
     * DTO for GitHub token response.
     */
    public record GitHubTokenResponse(
        String accessToken,
        String refreshToken,
        Integer expiresIn
    ) {}

    /**
     * List GitHub repositories for the authenticated user.
     *
     * @param accessToken The access token
     * @param page Page number (1-indexed)
     * @param perPage Items per page (1-100)
     * @return List of repositories with pagination info
     * @throws GitHubOAuthException if API call fails
     */
    public GitHubRepositoriesResponse listRepositories(String accessToken, int page, int perPage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "LearnHub/1.0");

            String url = String.format(
                "https://api.github.com/user/repos?page=%d&per_page=%d",
                page, perPage
            );

            HttpEntity<String> request = new HttpEntity<>(headers);

            log.debug("Fetching user repositories from GitHub");
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
            );

            if (response.getStatusCode().value() == 401) {
                log.warn("GitHub authorization failed (401) - token may be expired");
                throw new GitHubAPIException("GitHub authorization expired. Please reconnect your account.");
            }

            if (response.getStatusCode().value() == 403) {
                // Check if it's rate limit
                String remaining = response.getHeaders().getFirst("X-RateLimit-Remaining");
                String resetStr = response.getHeaders().getFirst("X-RateLimit-Reset");
                if ("0".equals(remaining)) {
                    log.warn("GitHub rate limit exceeded");
                    throw new GitHubRateLimitException(
                        "GitHub API rate limit exceeded",
                        Long.parseLong(resetStr != null ? resetStr : "0")
                    );
                }
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("GitHub repositories API failed: {} {}", response.getStatusCode(), response.getBody());
                throw new GitHubAPIException("GitHub API error: " + response.getStatusCode());
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                log.error("GitHub repositories endpoint returned empty response");
                throw new GitHubAPIException("GitHub returned empty response");
            }

            JsonNode reposArray = objectMapper.readTree(responseBody);
            if (!reposArray.isArray()) {
                log.error("GitHub repositories endpoint did not return an array");
                throw new GitHubAPIException("Invalid GitHub API response format");
            }

            var repos = new java.util.ArrayList<GitHubRepository>();
            for (JsonNode repoNode : reposArray) {
                repos.add(new GitHubRepository(
                    repoNode.get("id").asLong(),
                    repoNode.get("name").asText(),
                    repoNode.get("html_url").asText(),
                    repoNode.has("description") && !repoNode.get("description").isNull()
                        ? repoNode.get("description").asText()
                        : null,
                    repoNode.has("language") && !repoNode.get("language").isNull()
                        ? repoNode.get("language").asText()
                        : null,
                    repoNode.get("pushed_at").asText(),
                    repoNode.get("size").asInt(),
                    repoNode.get("private").asBoolean(),
                    repoNode.get("fork").asBoolean()
                ));
            }

            // Extract rate limit info
            String remaining = response.getHeaders().getFirst("X-RateLimit-Remaining");
            String limit = response.getHeaders().getFirst("X-RateLimit-Limit");
            String resetStr = response.getHeaders().getFirst("X-RateLimit-Reset");

            int remainingInt = remaining != null ? Integer.parseInt(remaining) : 5000;
            int limitInt = limit != null ? Integer.parseInt(limit) : 5000;
            long resetTimestamp = resetStr != null ? Long.parseLong(resetStr) : System.currentTimeMillis() / 1000;

            String linkHeader = response.getHeaders().getFirst("Link");
            boolean hasNextPage = linkHeader != null && linkHeader.contains("rel=\"next\"");

            log.info("Retrieved {} repositories from GitHub", repos.size());
            return new GitHubRepositoriesResponse(repos, remainingInt, limitInt, resetTimestamp, hasNextPage);

        } catch (GitHubRateLimitException e) {
            throw e;
        } catch (RestClientException | RuntimeException e) {
            log.error("Failed to fetch GitHub repositories", e);
            throw new GitHubAPIException("Failed to fetch repositories from GitHub", e);
        }
    }

    /**
     * DTO for GitHub repository.
     */
    public record GitHubRepository(
        Long id,
        String name,
        String url,
        String description,
        String language,
        String lastUpdated,
        Integer sizeKb,
        Boolean isPrivate,
        Boolean isFork
    ) {}

    /**
     * DTO for GitHub repositories response.
     */
    public record GitHubRepositoriesResponse(
        java.util.List<GitHubRepository> repositories,
        Integer rateLimitRemaining,
        Integer rateLimitLimit,
        Long rateLimitReset,
        Boolean hasNextPage
    ) {}

    /**
     * DTO for GitHub user profile.
     */
    public record GitHubUserProfile(
        Long githubUserId,
        String githubUsername,
        String email,
        String avatarUrl
    ) {}
}
