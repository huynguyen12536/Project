-- V12: Phase 1A Critical Path Indexes
-- ============================================================================
-- PURPOSE:
-- Creates performance indexes for all Phase 1A critical query paths.
-- All index creations use IF NOT EXISTS to be idempotent (safe to re-run).
--
-- INDEX STRATEGY:
--   - Unique indexes on natural keys that are not already unique constraints
--   - Partial indexes on active/non-expired subsets for high-cardinality tables
--   - Composite indexes matching the most common query predicates
-- ============================================================================

-- ============================================================================
-- USERS table indexes
-- ============================================================================

-- Unique email lookup (registration, login, password reset)
-- NOTE: idx_users_email was created in V1 as non-unique; recreate as UNIQUE
-- The UNIQUE constraint on users.email in V1 already enforces uniqueness,
-- but an explicit unique index makes the constraint name predictable.
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_unique
    ON users(email)
    WHERE deleted_at IS NULL;

-- Username lookup for profile pages and @mentions
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_unique
    ON users(username)
    WHERE username IS NOT NULL AND deleted_at IS NULL;

-- Active users ordered by creation date (admin dashboard, analytics)
CREATE INDEX IF NOT EXISTS idx_users_active_created
    ON users(created_at DESC)
    WHERE deleted_at IS NULL;

-- Email verification status for notification jobs
CREATE INDEX IF NOT EXISTS idx_users_email_verified
    ON users(email_verified)
    WHERE deleted_at IS NULL;

-- ============================================================================
-- REFRESH TOKENS table indexes
-- ============================================================================

-- Token hash lookup (every authenticated request)
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_hash
    ON refresh_tokens(token_hash);

-- Active tokens per user (session listing, revoke-all)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_active
    ON refresh_tokens(user_id, expires_at DESC)
    WHERE revoked = FALSE AND deleted_at IS NULL;

-- Expired token cleanup job
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at
    ON refresh_tokens(expires_at)
    WHERE revoked = FALSE;

-- ============================================================================
-- EMAIL VERIFICATION TOKENS table indexes
-- ============================================================================

-- Token lookup during email verification flow
CREATE UNIQUE INDEX IF NOT EXISTS idx_email_verification_token_lookup
    ON email_verification_tokens(token)
    WHERE deleted_at IS NULL;

-- Cleanup job: find expired unverified tokens
CREATE INDEX IF NOT EXISTS idx_email_verification_expires
    ON email_verification_tokens(expires_at)
    WHERE verified_at IS NULL AND deleted_at IS NULL;

-- ============================================================================
-- PASSWORD RESET TOKENS table indexes
-- ============================================================================

-- Token lookup during password reset flow
CREATE UNIQUE INDEX IF NOT EXISTS idx_password_reset_token_lookup
    ON password_reset_tokens(token)
    WHERE deleted_at IS NULL;

-- Cleanup job: find expired unused tokens
CREATE INDEX IF NOT EXISTS idx_password_reset_expires
    ON password_reset_tokens(expires_at)
    WHERE used_at IS NULL AND deleted_at IS NULL;

-- User's active reset tokens (prevent token-flood attacks)
CREATE INDEX IF NOT EXISTS idx_password_reset_user_active
    ON password_reset_tokens(user_id, created_at DESC)
    WHERE used_at IS NULL AND deleted_at IS NULL;

-- ============================================================================
-- ACCOUNT LOCKOUTS table indexes
-- ============================================================================

-- Active lockout check (every failed login attempt)
CREATE INDEX IF NOT EXISTS idx_account_lockouts_active
    ON account_lockouts(user_id, locked_until)
    WHERE deleted_at IS NULL;

-- ============================================================================
-- AUDIT TRAIL table indexes (supplementary to V11)
-- ============================================================================

-- Time-range scans for compliance reports
CREATE INDEX IF NOT EXISTS idx_audit_trail_resource_time
    ON audit_trail(resource_type, occurred_at DESC);

-- ============================================================================
-- USER CONSENTS table indexes (supplementary to V11)
-- ============================================================================

-- Latest consent per type per user (most common read pattern)
CREATE INDEX IF NOT EXISTS idx_user_consents_latest
    ON user_consents(user_id, consent_type, granted_at DESC);

-- ============================================================================
-- ASSESSMENTS table indexes (supplementary — common query paths)
-- ============================================================================

-- Find assessments in a specific status (job queue polling)
CREATE INDEX IF NOT EXISTS idx_assessments_status_active
    ON assessments(status, created_at DESC)
    WHERE deleted_at IS NULL;

-- User's assessment history
CREATE INDEX IF NOT EXISTS idx_assessments_user_active
    ON assessments(user_id, created_at DESC)
    WHERE deleted_at IS NULL;
