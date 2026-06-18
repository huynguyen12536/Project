package com.learnhub.github.controller;

import com.learnhub.auth.oauth.exception.GitHubAPIException;
import com.learnhub.auth.oauth.exception.GitHubRateLimitException;
import com.learnhub.github.dto.GitHubRepositoryResponse;
import com.learnhub.github.service.GitHubRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/github")
@PreAuthorize("hasRole('LEARNER')")
@Slf4j
public class GitHubRepositoryController {

    private final GitHubRepositoryService gitHubRepositoryService;

    public GitHubRepositoryController(GitHubRepositoryService gitHubRepositoryService) {
        this.gitHubRepositoryService = gitHubRepositoryService;
    }

    /**
     * List repositories for authenticated user's GitHub account.
     *
     * Requires: LEARNER role + active GitHub OAuth connection
     *
     * GitHub Access Token Handling (Temporary for Story 2.2):
     *
     * Current Implementation: Token passed via X-GitHub-Token header
     * This is a temporary solution for Story 2.2 development.
     *
     * Future Implementation (Story 2.3+):
     * - Retrieve token from UserOAuth entity in database
     * - Use authenticated user ID to look up their saved GitHub token
     * - Remove X-GitHub-Token header requirement
     *
     * Security Note:
     * While header transmission works for MVP, passing tokens in headers is not ideal
     * for production. Once UserOAuth is integrated, the token will be stored securely
     * in the database and accessed server-side only.
     *
     * @param perPage Number of repos per page (1-100, default 30)
     * @param page Page number (1-indexed, default 1)
     * @param refresh Force refresh from GitHub (bypass cache)
     * @param githubAccessToken GitHub access token from X-GitHub-Token header
     * @return Paginated repositories with rate limit info
     */
    @GetMapping("/repositories")
    public ResponseEntity<GitHubRepositoryResponse> listRepositories(
        @RequestParam(defaultValue = "30") int per_page,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "false") boolean refresh,
        @RequestHeader(name = "X-GitHub-Token", required = false) String githubAccessToken
    ) {
        log.info("Listing repositories for user (page={}, per_page={}, refresh={})", page, per_page, refresh);

        // Validate pagination parameters
        if (per_page < 1 || per_page > 100) {
            return ResponseEntity.badRequest().build();
        }
        if (page < 1) {
            return ResponseEntity.badRequest().build();
        }

        // TODO: Get token from UserOAuth entity instead of header once UserOAuth is implemented
        if (githubAccessToken == null || githubAccessToken.isBlank()) {
            log.warn("GitHub access token not provided");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            GitHubRepositoryResponse response = gitHubRepositoryService.listRepositories(
                githubAccessToken,
                page,
                per_page,
                refresh
            );
            return ResponseEntity.ok(response);

        } catch (GitHubRateLimitException e) {
            log.warn("GitHub rate limit exceeded: {}", e.getMessage());
            // Let GlobalExceptionHandler format response with rate limit details
            throw e;
        } catch (GitHubAPIException e) {
            log.error("GitHub API error: {}", e.getMessage());
            // Let GlobalExceptionHandler format response
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error listing repositories", e);
            // Let GlobalExceptionHandler format response
            throw e;
        }
    }
}
