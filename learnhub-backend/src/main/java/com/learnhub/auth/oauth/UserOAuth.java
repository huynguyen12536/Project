package com.learnhub.auth.oauth;

import com.learnhub.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * OAuth connection metadata for users.
 * Stores secure references to external OAuth providers (GitHub, Google, etc.)
 * and associated token information.
 *
 * Security: Tokens are stored as HMAC-SHA256 hashes. Plain tokens are never persisted.
 */
@Entity
@Table(
    name = "user_oauth",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "oauth_provider"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;

    // GitHub-specific fields
    @Column(name = "github_username")
    private String githubUsername;

    @Column(name = "github_user_id", unique = true)
    private Long githubUserId;

    @Column(name = "email")
    private String email;

    // Token information (always stored as hash, never plaintext)
    @Column(name = "access_token_hash", nullable = false)
    private String accessTokenHash;

    @Column(name = "refresh_token_hash")
    private String refreshTokenHash;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    // OAuth scopes granted by user
    @Column(name = "scopes", columnDefinition = "jsonb")
    @Convert(disableConversion = true)  // Let Hibernate handle as JSON natively
    private List<String> scopes;  // e.g., ["repo", "user:email"]

    // Connection status
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "connected_at", nullable = false, updatable = false)
    private LocalDateTime connectedAt;

    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============
    // Business Logic Methods
    // ============

    /**
     * Check if the OAuth token has expired.
     */
    public boolean isTokenExpired() {
        return tokenExpiresAt != null && LocalDateTime.now().isAfter(tokenExpiresAt);
    }

    /**
     * Check if token needs refresh (expires within 5 minutes).
     * This allows proactive refresh before actual expiration.
     */
    public boolean needsRefresh() {
        if (tokenExpiresAt == null) return false;
        LocalDateTime bufferTime = tokenExpiresAt.minusMinutes(5);
        return LocalDateTime.now().isAfter(bufferTime);
    }

    /**
     * Mark this connection as last used.
     */
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Mark this connection as disconnected.
     */
    public void disconnect() {
        this.isActive = false;
        this.disconnectedAt = LocalDateTime.now();
    }

    /**
     * Reactivate a previously disconnected connection.
     */
    public void reconnect() {
        this.isActive = true;
        this.disconnectedAt = null;
    }
}
