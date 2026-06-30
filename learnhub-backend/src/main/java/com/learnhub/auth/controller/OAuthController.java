package com.learnhub.auth.controller;

import com.learnhub.auth.model.RefreshToken;
import com.learnhub.auth.oauth.GitHubOAuthService;
import com.learnhub.auth.oauth.dto.UserOAuthResponse;
import com.learnhub.auth.repository.RefreshTokenRepository;
import com.learnhub.auth.service.JwtService;
import com.learnhub.auth.service.TokenHashService;
import com.learnhub.common.util.AuthenticationUtil;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * GitHub OAuth endpoints under /api/v1/auth/github/ namespace (Phase 1A spec).
 *
 * ENDPOINT 8:  POST /api/v1/auth/github/authorize — Initiate OAuth flow
 * ENDPOINT 9:  GET  /api/v1/auth/github/callback  — Handle GitHub callback
 *
 * Delegates to GitHubOAuthService for business logic.
 * On callback: issues JWT + refresh token pair and redirects to frontend.
 *
 * Security notes:
 * - State token validated atomically (single-use, time-limited)
 * - On new user via GitHub: creates account automatically
 * - GitHub token stored encrypted (AES-256-GCM via TokenEncryptor in service)
 * - No PII logged — userId only
 */
@RestController
@RequestMapping("/api/v1/auth/github")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {

    private final GitHubOAuthService gitHubOAuthService;
    private final AuthenticationUtil authenticationUtil;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final UserRepository userRepository;

    // =========================================================================
    // ENDPOINT 8: POST /api/v1/auth/github/authorize
    // =========================================================================

    /**
     * Initiate GitHub OAuth authorization.
     * Requires authenticated user (LEARNER role).
     *
     * Returns: { authorizationUrl: "https://github.com/login/oauth/authorize?..." }
     */
    @PostMapping("/authorize")
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<Map<String, String>> authorize() {
        UUID userId = authenticationUtil.getCurrentUserId();
        try {
            String redirectUrl = gitHubOAuthService.initiateAuthorization(userId);
            Map<String, String> response = new HashMap<>();
            response.put("authorizationUrl", redirectUrl);
            log.info("GitHub OAuth authorization initiated for userId: {}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to initiate GitHub OAuth for userId: {}", userId, e);
            throw e;
        }
    }

    // =========================================================================
    // ENDPOINT 9: GET /api/v1/auth/github/callback
    // =========================================================================

    /**
     * Handle GitHub OAuth callback.
     * GitHub redirects here with authorization code and state token.
     *
     * Validates state atomically, exchanges code for token, creates/links user account,
     * then redirects to frontend with JWT + refresh token.
     *
     * Errors:
     * - 400: State mismatch or expired
     * - 503: GitHub API error (rate limit, timeout)
     */
    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(
        @RequestParam @NotBlank String code,
        @RequestParam @NotBlank @Size(min = 64, max = 64) String state
    ) {
        try {
            // Complete OAuth flow — service handles state validation + user creation/linking
            UserOAuthResponse oauthResponse = gitHubOAuthService.handleCallbackFromState(code, state);

            // Get the user to issue JWT
            UUID userId = oauthResponse.userId();
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isEmpty()) {
                log.error("User not found after OAuth callback for userId: {}", userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error_code", "USER_NOT_FOUND", "message", "User creation failed"));
            }

            User user = userOpt.get();

            // Issue JWT + refresh token
            String accessToken = jwtService.createAccessToken(userId.toString(), user.getRole());
            String refreshToken = jwtService.createRefreshToken(userId.toString());

            // Persist refresh token
            String hash = tokenHashService.hashToken(refreshToken);
            RefreshToken rt = new RefreshToken();
            rt.setUserId(userId);
            rt.setTokenHash(hash);
            rt.setIssuedAt(Instant.now());
            rt.setExpiresAt(Instant.now().plusSeconds(2592000));
            rt.setRevoked(false);
            refreshTokenRepository.save(rt);

            log.info("GitHub OAuth callback completed for userId: {}", userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("github_username", oauthResponse.githubUsername());
            response.put("message", "GitHub account connected successfully");

            return ResponseEntity.ok(response);

        } catch (com.learnhub.auth.oauth.exception.StateTokenMismatchException e) {
            log.warn("GitHub OAuth state mismatch");
            return ResponseEntity.badRequest()
                .body(Map.of("error_code", "STATE_MISMATCH", "message", e.getMessage()));
        } catch (com.learnhub.auth.oauth.exception.GitHubRateLimitException e) {
            log.warn("GitHub API rate limit during OAuth callback");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error_code", "GITHUB_RATE_LIMIT", "message", "GitHub API rate limit exceeded. Please try again."));
        } catch (com.learnhub.auth.oauth.exception.GitHubAPIException e) {
            log.error("GitHub API error during OAuth callback");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error_code", "GITHUB_API_ERROR", "message", "GitHub service temporarily unavailable."));
        } catch (Exception e) {
            log.error("Unexpected error during GitHub OAuth callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error_code", "INTERNAL_ERROR", "message", "OAuth callback failed"));
        }
    }
}
