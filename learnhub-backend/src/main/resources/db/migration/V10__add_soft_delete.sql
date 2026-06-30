-- V10: Add Soft Delete Pattern
-- ============================================================================
-- PURPOSE:
-- Implements soft delete across all content tables using a deleted_at TIMESTAMPTZ
-- column. Rows are never physically deleted; instead deleted_at is set to the
-- deletion timestamp. All application queries MUST filter WHERE deleted_at IS NULL.
--
-- PATTERN: WHERE deleted_at IS NULL  →  active records
--          WHERE deleted_at IS NOT NULL  →  soft-deleted records
--
-- No ON DELETE CASCADE changes — foreign key cascades are NOT added to prevent
-- accidental hard-deletes from propagating through the soft-delete hierarchy.
-- ============================================================================

-- Users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Repositories table
ALTER TABLE repositories ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Repository snapshots table
ALTER TABLE repository_snapshots ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Assessments table
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Assessment results table
ALTER TABLE assessment_results ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Refresh tokens table (supports token revocation audit trail)
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Email verification tokens (allow soft-invalidation without data loss)
ALTER TABLE email_verification_tokens ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Password reset tokens (allow soft-invalidation without data loss)
ALTER TABLE password_reset_tokens ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Account lockouts (allow soft-release without data loss)
ALTER TABLE account_lockouts ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- ============================================================================
-- PARTIAL INDEXES for efficient soft-delete queries
-- Partial indexes only index ACTIVE rows, making WHERE deleted_at IS NULL
-- queries fast on large tables without indexing deleted rows.
-- ============================================================================

-- Primary active-record indexes (partial indexes on active rows)
CREATE INDEX IF NOT EXISTS idx_users_soft_delete
  ON users(created_at DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_repositories_soft_delete
  ON repositories(user_id, created_at DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_repository_snapshots_soft_delete
  ON repository_snapshots(user_id, created_at DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_assessments_soft_delete
  ON assessments(user_id, created_at DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_assessment_results_soft_delete
  ON assessment_results(assessment_id)
  WHERE deleted_at IS NULL;

-- Deleted records index for GDPR data export / admin queries
CREATE INDEX IF NOT EXISTS idx_users_deleted
  ON users(deleted_at)
  WHERE deleted_at IS NOT NULL;
