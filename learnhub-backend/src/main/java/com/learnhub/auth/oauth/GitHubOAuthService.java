package com.learnhub.auth.oauth;

import com.learnhub.auth.oauth.dto.UserOAuthResponse;
import com.learnhub.auth.oauth.exception.*;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for GitHub OAuth operations.
 * Handles authorization flow, token storage, refresh, and disconnection.
 *
 * Security Notes:
 * - All tokens are hashed before storage (HMAC-SHA256)
 * - State tokens are single-use and time-limited
 * - Tokens are refreshed proactively before expiration
 */
@Service
@Slf4j
public class GitHubOAuthService {

    private final UserOAuthRepository userOAuthRepository;
    private final UserRepository userRepository;
    private final GitHubApiClient gitHubApiClient;
    private final StateTokenGenerator stateTokenGenerator;
    private final TokenHasher tokenHasher;
    private final TokenEncryptor tokenEncryptor;
    private final GitHubOAuthProperties properties;

    public GitHubOAuthService(
        UserOAuthRepository userOAuthRepository,
        UserRepository userRepository,
        GitHubApiClient gitHubApiClient,
        StateTokenGenerator stateTokenGenerator,
        TokenHasher tokenHasher,
        TokenEncryptor tokenEncryptor,
        GitHubOAuthProperties properties
    ) {
        this.userOAuthRepository = userOAuthRepository;
        this.userRepository = userRepository;
        this.gitHubApiClient = gitHubApiClient;
        this.stateTokenGenerator = stateTokenGenerator;
        this.tokenHasher = tokenHasher;
        this.tokenEncryptor = tokenEncryptor;
        this.properties = properties;
    }

    /**
     * Initiate GitHub OAuth flow by generating state token and redirect URL.
     *
     * @param userId The authenticated user initiating the connection
     * @return The GitHub authorization URL
     */
    @Transactional(readOnly = true)
    public String initiateAuthorization(UUID userId) {
        // Verify user exists and is authenticated
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Generate CSRF protection token bound to user ID
        String stateToken = stateTokenGenerator.generateWithUserId(userId);

        // Build redirect URL
        String redirectUrl = String.format(
            "%s?client_id=%s&redirect_uri=%s&scope=%s&state=%s&allow_signup=true",
            properties.getAuthUrl(),
            properties.getClientId(),
            properties.getRedirectUri(),
            String.join(",", properties.getScopes()),
            stateToken
        );

        log.info("Initiated GitHub OAuth flow for user: {}", userId);
        return redirectUrl;
    }

    /**
     * Handle GitHub OAuth callback from unauthenticated redirect.
     * Extracts user ID from state token and completes OAuth flow.
     *
     * @param code The authorization code from GitHub
     * @param state The state token from GitHub callback (bound to user ID)
     * @return The stored OAuth connection information
     * @throws StateTokenMismatchException if state is invalid or expired
     * @throws InvalidAuthorizationCodeException if code is invalid
     * @throws DuplicateGitHubUserException if GitHub account already linked
     */
    @Transactional
    public UserOAuthResponse handleCallbackFromState(String code, String state) {
        // Extract user ID from state token
        Optional<UUID> userIdOptional = stateTokenGenerator.validateAndGetUserId(state);
        if (userIdOptional.isEmpty()) {
            log.warn("Failed to extract user ID from state token");
            throw new StateTokenMismatchException("Invalid or expired state token");
        }

        UUID userId = userIdOptional.get();

        // Verify user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // State token already validated by validateAndGetUserId, proceed with token exchange

        // Exchange authorization code for access token
        GitHubApiClient.GitHubTokenResponse tokenResponse = gitHubApiClient.exchangeCodeForToken(code);

        // Fetch user profile from GitHub
        GitHubApiClient.GitHubUserProfile profile = gitHubApiClient.getUserProfile(tokenResponse.accessToken());

        // Check for duplicate GitHub user (another LearnHub user already has this GitHub account)
        if (userOAuthRepository.existsByGithubUserIdAndUserIdNotAndIsActiveTrue(profile.githubUserId(), userId)) {
            log.warn("GitHub user {} already linked to another account", profile.githubUserId());
            throw new DuplicateGitHubUserException(
                "This GitHub account is already linked to another LearnHub account"
            );
        }

        // Check if user already has an active GitHub connection
        Optional<UserOAuth> existingConnection = userOAuthRepository.findByUserIdAndProvider(
            userId, OAuthProvider.GITHUB
        );

        UserOAuth userOAuth;
        if (existingConnection.isPresent()) {
            // Update existing connection
            userOAuth = existingConnection.get();
            log.info("Updating existing GitHub connection for user: {}", userId);
        } else {
            // Create new connection
            userOAuth = new UserOAuth();
            userOAuth.setUser(user);
            userOAuth.setProvider(OAuthProvider.GITHUB);
            log.info("Creating new GitHub connection for user: {}", userId);
        }

        // Store access token as hash (short-lived, never decrypted)
        userOAuth.setAccessTokenHash(tokenHasher.hash(tokenResponse.accessToken()));

        // Store refresh token as encrypted (long-lived, must be decryptable for GitHub API calls)
        if (tokenResponse.refreshToken() != null) {
            userOAuth.setRefreshTokenHash(tokenEncryptor.encrypt(tokenResponse.refreshToken()));
        }

        // Calculate expiration time
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.expiresIn());
        userOAuth.setTokenExpiresAt(expiresAt);

        // Store GitHub user information
        userOAuth.setGithubUserId(profile.githubUserId());
        userOAuth.setGithubUsername(profile.githubUsername());
        userOAuth.setEmail(profile.email());

        // Store scopes
        userOAuth.setScopes(properties.getScopes());

        // Mark as active
        userOAuth.setIsActive(true);
        userOAuth.setConnectedAt(LocalDateTime.now());

        // Persist
        userOAuthRepository.save(userOAuth);

        log.info("Successfully linked GitHub user {} to LearnHub user {}", profile.githubUsername(), userId);

        return new UserOAuthResponse(
            userId,
            userOAuth.getGithubUsername(),
            profile.avatarUrl(),
            userOAuth.getConnectedAt()
        );
    }

    /**
     * Refresh an expired or expiring GitHub access token.
     * Decrypts the stored refresh token, calls GitHub API, and updates both tokens.
     *
     * @param userId The user whose token should be refreshed
     * @throws OAuthNotConnectedException if user has no GitHub connection
     * @throws TokenRefreshException if refresh fails
     */
    @Transactional
    public void refreshToken(UUID userId) {
        UserOAuth userOAuth = userOAuthRepository.findByUserIdAndProvider(userId, OAuthProvider.GITHUB)
            .orElseThrow(() -> new OAuthNotConnectedException("No GitHub connection found for user: " + userId));

        if (!userOAuth.getIsActive()) {
            throw new OAuthNotConnectedException("GitHub connection is not active");
        }

        if (userOAuth.getRefreshTokenHash() == null) {
            log.warn("Cannot refresh token for user {} - no refresh token available", userId);
            throw new TokenRefreshException("No refresh token available");
        }

        try {
            // Decrypt the refresh token
            String decryptedRefreshToken = tokenEncryptor.decrypt(userOAuth.getRefreshTokenHash());

            // Call GitHub API to refresh
            GitHubApiClient.GitHubTokenResponse newTokenResponse =
                gitHubApiClient.refreshAccessToken(decryptedRefreshToken);

            // Update tokens
            userOAuth.setAccessTokenHash(tokenHasher.hash(newTokenResponse.accessToken()));

            // Re-encrypt new refresh token if provided
            if (newTokenResponse.refreshToken() != null) {
                userOAuth.setRefreshTokenHash(tokenEncryptor.encrypt(newTokenResponse.refreshToken()));
            }

            // Update expiration
            LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(newTokenResponse.expiresIn());
            userOAuth.setTokenExpiresAt(newExpiresAt);

            // Save updated tokens
            userOAuthRepository.save(userOAuth);

            log.info("Successfully refreshed GitHub access token for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to refresh GitHub token for user: {}", userId, e);
            throw new TokenRefreshException("Failed to refresh GitHub token: " + e.getMessage());
        }
    }

    /**
     * Disconnect GitHub OAuth connection.
     *
     * @param userId The user disconnecting their GitHub account
     */
    @Transactional
    public void disconnectGitHub(UUID userId) {
        UserOAuth userOAuth = userOAuthRepository.findByUserIdAndProvider(userId, OAuthProvider.GITHUB)
            .orElseThrow(() -> new OAuthNotConnectedException("No GitHub connection found for user: " + userId));

        userOAuth.disconnect();
        userOAuthRepository.save(userOAuth);

        log.info("Disconnected GitHub account for user: {}", userId);
    }

    /**
     * Get active GitHub OAuth connection for a user.
     *
     * @param userId The user
     * @return Optional containing the connection if active
     */
    @Transactional(readOnly = true)
    public Optional<UserOAuth> getActiveConnection(UUID userId) {
        return userOAuthRepository.findByUserIdAndProvider(userId, OAuthProvider.GITHUB)
            .filter(UserOAuth::getIsActive);
    }

    /**
     * Validate that user has an active GitHub connection.
     *
     * @param userId The user
     * @throws OAuthNotConnectedException if user has no active connection
     */
    @Transactional(readOnly = true)
    public void validateConnectionExists(UUID userId) {
        UserOAuth userOAuth = userOAuthRepository.findByUserIdAndProvider(userId, OAuthProvider.GITHUB)
            .orElseThrow(() -> new OAuthNotConnectedException("No GitHub connection found for user: " + userId));

        if (!userOAuth.getIsActive()) {
            throw new OAuthNotConnectedException("GitHub connection is not active");
        }
    }

    /**
     * Scheduled job to refresh tokens before expiration.
     * Runs every hour to proactively refresh tokens.
     * Tokens are refreshed 5 minutes before expiration to ensure continuity.
     */
    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000) // 1 hour
    @Transactional
    public void refreshExpiringTokens() {
        log.debug("Starting token refresh scheduler job");

        LocalDateTime bufferTime = LocalDateTime.now().plusMinutes(
            properties.getTokenExpiryBufferMinutes()
        );

        List<UserOAuth> expiringTokens = userOAuthRepository
            .findByIsActiveTrueAndTokenExpiresAtBeforeAndTokenExpiresAtIsNotNull(bufferTime);

        log.info("Found {} expiring tokens to refresh", expiringTokens.size());

        for (UserOAuth userOAuth : expiringTokens) {
            try {
                UUID userId = userOAuth.getUser().getId();

                if (userOAuth.getRefreshTokenHash() != null) {
                    // Refresh the token using encrypted refresh token
                    log.debug("Refreshing token for user: {}", userId);
                    refreshToken(userId);
                    log.info("Successfully refreshed token for user: {}", userId);
                } else {
                    log.warn("Token expiring but no refresh token available for user: {}", userId);
                    userOAuth.setIsActive(false);
                    userOAuthRepository.save(userOAuth);
                }
            } catch (Exception e) {
                log.error("Failed to refresh token for user: {}", userOAuth.getUser().getId(), e);
                // Don't throw - continue with other tokens
            }
        }

        log.debug("Token refresh scheduler job completed");
    }
}
