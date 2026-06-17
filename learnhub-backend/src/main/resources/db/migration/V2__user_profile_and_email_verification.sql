-- V2: User Profile Management and Email Verification
-- Adds profile fields to users table
-- Creates email verification tokens table
-- Creates password reset tokens table
-- Creates account lockout table

-- ============================================================================
-- ALTER users TABLE: Add Profile and Security Fields
-- ============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_picture_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS location VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS github_profile_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_password_change TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_change_required BOOLEAN DEFAULT FALSE;

-- Add indexes for frequently queried columns
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);

-- ============================================================================
-- CREATE email_verification_tokens TABLE
-- Stores email verification tokens for new user registrations
-- ============================================================================

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    token VARCHAR(255) NOT NULL UNIQUE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP,
    is_expired BOOLEAN DEFAULT FALSE,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_id
    ON email_verification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_token_hash
    ON email_verification_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_expires_at
    ON email_verification_tokens(expires_at);

-- ============================================================================
-- CREATE password_reset_tokens TABLE
-- Stores password reset tokens for password recovery flow
-- ============================================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP,
    is_used BOOLEAN DEFAULT FALSE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id
    ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token_hash
    ON password_reset_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expires_at
    ON password_reset_tokens(expires_at);

-- ============================================================================
-- CREATE account_lockouts TABLE
-- Tracks failed login attempts and account lockout state
-- ============================================================================

CREATE TABLE IF NOT EXISTS account_lockouts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    locked_until TIMESTAMP NOT NULL,
    failed_attempts INT DEFAULT 0,
    last_failed_attempt TIMESTAMP,
    lock_reason VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_account_lockouts_user_id
    ON account_lockouts(user_id);
CREATE INDEX IF NOT EXISTS idx_account_lockouts_locked_until
    ON account_lockouts(locked_until);

-- ============================================================================
-- Comments for documentation
-- ============================================================================

COMMENT ON TABLE email_verification_tokens IS
    'Stores email verification tokens for new user registrations. One active token per user at a time.';

COMMENT ON COLUMN email_verification_tokens.token_hash IS
    'Hash of the token for secure storage. Token is sent via email, hash stored in DB for verification.';

COMMENT ON TABLE password_reset_tokens IS
    'Stores password reset tokens for account recovery. Multiple reset tokens can exist per user.';

COMMENT ON TABLE account_lockouts IS
    'Tracks account lockout state after failed login attempts. Used for brute force protection.';

-- ============================================================================
-- Set NOT NULL constraints for profile completion
-- ============================================================================

-- Ensure email verification is tracked
ALTER TABLE users ALTER COLUMN email_verified SET NOT NULL;

-- ============================================================================
-- ROLLBACK
-- In case of issues, the following can be used to revert changes:
--
-- DROP TABLE IF EXISTS account_lockouts CASCADE;
-- DROP TABLE IF EXISTS password_reset_tokens CASCADE;
-- DROP TABLE IF EXISTS email_verification_tokens CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS email_verified CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS email_verified_at CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS first_name CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS last_name CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS bio CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS profile_picture_url CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS phone CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS location CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS github_profile_url CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS last_login CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS last_password_change CASCADE;
-- ALTER TABLE users DROP COLUMN IF EXISTS password_change_required CASCADE;
-- ============================================================================
