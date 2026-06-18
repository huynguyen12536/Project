package com.learnhub.auth.oauth;

import com.learnhub.auth.oauth.dto.UserOAuthResponse;
import com.learnhub.common.util.AuthenticationUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for GitHub OAuth operations.
 * Handles authorization initiation, callback processing, disconnection, and status checking.
 *
 * Endpoints:
 * - GET /api/v1/oauth/github/authorize - Initiate OAuth flow
 * - GET /api/v1/oauth/github/callback - Handle GitHub callback
 * - DELETE /api/v1/oauth/github/disconnect - Disconnect GitHub account
 * - GET /api/v1/oauth/github/status - Check connection status
 */
@RestController
@RequestMapping("/api/v1/oauth")
@Slf4j
public class GitHubOAuthController {

    private final GitHubOAuthService gitHubOAuthService;
    private final AuthenticationUtil authenticationUtil;

    public GitHubOAuthController(
        GitHubOAuthService gitHubOAuthService,
        AuthenticationUtil authenticationUtil
    ) {
        this.gitHubOAuthService = gitHubOAuthService;
        this.authenticationUtil = authenticationUtil;
    }

    /**
     * Initiate GitHub OAuth authorization flow.
     * Returns redirect URL to GitHub consent screen.
     *
     * @return Redirect URL for GitHub OAuth
     */
    @GetMapping("/github/authorize")
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<Map<String, String>> authorize() {
        UUID userId = authenticationUtil.getCurrentUserId();

        try {
            String redirectUrl = gitHubOAuthService.initiateAuthorization(userId);

            Map<String, String> response = new HashMap<>();
            response.put("redirectUrl", redirectUrl);

            log.info("GitHub OAuth authorization initiated for user: {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to initiate GitHub OAuth for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * Handle GitHub OAuth callback.
     * GitHub redirects here after user approves/denies permissions.
     * User ID is extracted from the state token (no authentication required).
     *
     * @param code The authorization code from GitHub
     * @param state The state token for CSRF protection (bound to user ID)
     * @return OAuth connection information
     */
    @GetMapping("/github/callback")
    public ResponseEntity<Map<String, Object>> handleCallback(
        @RequestParam @NotBlank(message = "Authorization code is required")
            @Size(min = 1, max = 512, message = "Authorization code has invalid length")
            String code,
        @RequestParam @NotBlank(message = "State token is required")
            @Size(min = 64, max = 64, message = "State token must be exactly 64 characters")
            String state
    ) {
        try {
            // Extract user ID from state token and complete OAuth flow
            UserOAuthResponse oauthResponse = gitHubOAuthService.handleCallbackFromState(code, state);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("github_username", oauthResponse.githubUsername());
            response.put("avatar_url", oauthResponse.avatarUrl());
            response.put("connected_at", oauthResponse.connectedAt());
            response.put("message", "GitHub account successfully connected");

            log.info("GitHub OAuth callback processed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to process GitHub OAuth callback", e);
            throw e;
        }
    }

    /**
     * Disconnect GitHub OAuth connection.
     *
     * @return Success response
     */
    @DeleteMapping("/github/disconnect")
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<Map<String, String>> disconnect() {
        UUID userId = authenticationUtil.getCurrentUserId();

        try {
            gitHubOAuthService.disconnectGitHub(userId);

            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "GitHub account disconnected successfully");

            log.info("GitHub OAuth disconnected for user: {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to disconnect GitHub OAuth for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * Get GitHub OAuth connection status.
     *
     * @return Connection status and metadata
     */
    @GetMapping("/github/status")
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<Map<String, Object>> getStatus() {
        UUID userId = authenticationUtil.getCurrentUserId();

        try {
            var connection = gitHubOAuthService.getActiveConnection(userId);

            Map<String, Object> response = new HashMap<>();

            if (connection.isPresent()) {
                UserOAuth userOAuth = connection.get();
                response.put("connected", true);
                response.put("github_username", userOAuth.getGithubUsername());
                response.put("email", userOAuth.getEmail());
                response.put("connected_at", userOAuth.getConnectedAt());
                response.put("token_expires_at", userOAuth.getTokenExpiresAt());
                response.put("scopes", userOAuth.getScopes());
            } else {
                response.put("connected", false);
                response.put("message", "No GitHub connection found");
            }

            log.debug("GitHub OAuth status retrieved for user: {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get GitHub OAuth status for user: {}", userId, e);
            throw e;
        }
    }
}
