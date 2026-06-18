package com.learnhub.auth.oauth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserOAuth entity.
 * Provides data access operations for OAuth provider connections.
 */
@Repository
public interface UserOAuthRepository extends JpaRepository<UserOAuth, UUID> {

    /**
     * Find OAuth connection for a user by provider.
     */
    Optional<UserOAuth> findByUserIdAndProvider(UUID userId, OAuthProvider provider);

    /**
     * Find OAuth connection by GitHub user ID (for duplicate detection).
     */
    Optional<UserOAuth> findByGithubUserIdAndIsActiveTrue(Long githubUserId);

    /**
     * Find all active connections for a user.
     */
    List<UserOAuth> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Find all OAuth tokens that need refresh (expiring within buffer time).
     * Used by scheduled token refresh job.
     */
    List<UserOAuth> findByIsActiveTrueAndTokenExpiresAtBeforeAndTokenExpiresAtIsNotNull(
        LocalDateTime expiryThreshold
    );

    /**
     * Check if a GitHub user is already connected to a different LearnHub user.
     */
    boolean existsByGithubUserIdAndUserIdNotAndIsActiveTrue(Long githubUserId, UUID userId);

    /**
     * Check if user has an active OAuth connection with given provider.
     */
    boolean existsByUserIdAndProviderAndIsActiveTrue(UUID userId, OAuthProvider provider);

    /**
     * Delete all OAuth connections for a user (e.g., during account deletion).
     */
    void deleteByUserId(UUID userId);
}
