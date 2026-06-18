-- V3: GitHub OAuth Integration
-- Creates user_oauth table for storing OAuth provider connections
-- Supports GitHub and future providers (Google, GitLab)

-- ============================================================================
-- CREATE user_oauth TABLE
-- Stores OAuth provider metadata and secure token references
-- NOTE: Tokens are always stored as HMAC-SHA256 hashes, never plaintext
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_oauth (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    oauth_provider VARCHAR(50) NOT NULL,

    -- GitHub-specific fields
    github_username VARCHAR(255),
    github_user_id BIGINT UNIQUE,
    email VARCHAR(255),

    -- Token information (stored as hashes only)
    access_token_hash VARCHAR(255) NOT NULL,
    refresh_token_hash VARCHAR(255),
    token_expires_at TIMESTAMP,

    -- OAuth scopes granted by user (JSONB array)
    scopes JSONB DEFAULT '[]'::jsonb,

    -- Connection status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMP,
    last_used_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, oauth_provider)
);

-- ============================================================================
-- Indexes for Performance
-- ============================================================================

-- Index on github_user_id for rapid lookup when receiving GitHub webhooks
CREATE INDEX IF NOT EXISTS idx_user_oauth_github_id
    ON user_oauth(github_user_id)
    WHERE github_user_id IS NOT NULL;

-- Index on token_expires_at for token refresh scheduler job
CREATE INDEX IF NOT EXISTS idx_user_oauth_token_expires_at
    ON user_oauth(token_expires_at)
    WHERE is_active = true AND token_expires_at IS NOT NULL;

-- Index on is_active for listing active connections
CREATE INDEX IF NOT EXISTS idx_user_oauth_is_active
    ON user_oauth(is_active);

-- Index on user_id for quick lookup of user's OAuth connections
CREATE INDEX IF NOT EXISTS idx_user_oauth_user_id
    ON user_oauth(user_id);

-- ============================================================================
-- Verify migration
-- ============================================================================
-- Run SELECT to verify table exists with correct schema
-- SELECT * FROM user_oauth LIMIT 0;
