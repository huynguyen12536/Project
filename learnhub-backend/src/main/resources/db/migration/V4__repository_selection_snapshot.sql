-- V4: Repository Selection & Snapshot MVP
-- Creates tables for tracking user GitHub repository selections and snapshots
-- Supports repository selection tracking and point-in-time repository metadata snapshots

-- ============================================================================
-- CREATE user_github_selections TABLE
-- Tracks which GitHub repositories a user has selected for analysis/learning
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_github_selections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    github_repo_id BIGINT NOT NULL,
    selected_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, github_repo_id)
);

-- Index for fast lookup of selections by user
CREATE INDEX IF NOT EXISTS idx_user_github_selections_user_id
    ON user_github_selections(user_id);

-- ============================================================================
-- CREATE repository_snapshots TABLE
-- Stores point-in-time snapshots of GitHub repository metadata
-- Each snapshot captures repository state at a specific commit/branch
-- ============================================================================

CREATE TABLE IF NOT EXISTS repository_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    github_repo_id BIGINT NOT NULL,
    branch VARCHAR(255) NOT NULL DEFAULT 'main',
    commit_sha VARCHAR(40),
    files_count INT NOT NULL,
    total_size_kb BIGINT NOT NULL,
    languages JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for fast lookup of snapshots by user
CREATE INDEX IF NOT EXISTS idx_repository_snapshots_user_id
    ON repository_snapshots(user_id);

-- Index for fast lookup of snapshots by repository
CREATE INDEX IF NOT EXISTS idx_repository_snapshots_github_repo_id
    ON repository_snapshots(github_repo_id);

-- ============================================================================
-- Verify migration
-- ============================================================================
-- SELECT COUNT(*) FROM user_github_selections;
-- SELECT COUNT(*) FROM repository_snapshots;
