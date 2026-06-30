-- V11: Phase 1A Core Tables
-- ============================================================================
-- PURPOSE:
-- Creates the remaining Phase 1A tables for security, compliance, and audit:
--   - audit_trail: Immutable append-only event log for all state changes
--   - user_consents: GDPR consent tracking per user
--
-- NOTE: email_verification_tokens, password_reset_tokens, and account_lockouts
-- were already created in V2__user_profile_and_email_verification.sql and are
-- NOT re-created here.
-- ============================================================================

-- ============================================================================
-- TABLE: audit_trail
-- Purpose: Immutable append-only log of all significant state changes.
-- Design:  BIGSERIAL PK ensures total ordering. No UPDATE or DELETE is ever
--          issued by application code against this table. A PostgreSQL row-level
--          security policy (applied post-migration) will enforce immutability.
-- ============================================================================

CREATE TABLE IF NOT EXISTS audit_trail (
    id              BIGSERIAL PRIMARY KEY,
    actor_id        UUID,                          -- NULL for system-initiated actions
    actor_role      VARCHAR(50),                   -- Role at time of action (LEARNER, ADMIN, SYSTEM)
    action          VARCHAR(100) NOT NULL,          -- Verb: USER_REGISTERED, PASSWORD_CHANGED, etc.
    resource_type   VARCHAR(50)  NOT NULL,          -- Table/domain: users, assessments, etc.
    resource_id     UUID,                           -- PK of the affected row
    old_state       JSONB,                          -- Row state before change (NULL for INSERT)
    new_state       JSONB,                          -- Row state after change (NULL for DELETE)
    ip_address      INET,                           -- Client IP at time of action
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Audit trail indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_audit_trail_actor
    ON audit_trail(actor_id, occurred_at DESC)
    WHERE actor_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_trail_resource
    ON audit_trail(resource_type, resource_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_trail_action
    ON audit_trail(action, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_trail_occurred_at
    ON audit_trail(occurred_at DESC);

COMMENT ON TABLE audit_trail IS
    'Immutable append-only audit log. Application MUST NOT issue UPDATE or DELETE against this table.';

COMMENT ON COLUMN audit_trail.actor_id IS
    'UUID of the user who performed the action. NULL for background system tasks.';

COMMENT ON COLUMN audit_trail.old_state IS
    'JSONB snapshot of the row before the change. NULL for INSERT events. Must NOT contain plaintext passwords or raw tokens.';

COMMENT ON COLUMN audit_trail.new_state IS
    'JSONB snapshot of the row after the change. NULL for DELETE events. Must NOT contain plaintext passwords or raw tokens.';

-- ============================================================================
-- TABLE: user_consents
-- Purpose: GDPR-compliant tracking of user consent grants and revocations.
-- Design:  One row per consent event (grant or revoke). History is preserved
--          by adding new rows, never updating old ones.
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_consents (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type    VARCHAR(50) NOT NULL,   -- TERMS_OF_SERVICE, PRIVACY_POLICY, MARKETING, DATA_PROCESSING
    version         VARCHAR(20) NOT NULL,   -- Policy document version at time of consent: "1.0", "2.1"
    granted         BOOLEAN     NOT NULL,   -- TRUE = granted, FALSE = revoked
    ip_address      INET,
    user_agent      TEXT,
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at      TIMESTAMPTZ            -- Set when granted = FALSE (revocation timestamp)
);

-- Indexes for consent queries
CREATE INDEX IF NOT EXISTS idx_user_consents_user_id
    ON user_consents(user_id, consent_type, granted_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_consents_type_version
    ON user_consents(consent_type, version, granted_at DESC);

COMMENT ON TABLE user_consents IS
    'GDPR consent tracking. Each row is immutable; revocations create a new row with granted=FALSE.';

COMMENT ON COLUMN user_consents.consent_type IS
    'Consent category: TERMS_OF_SERVICE, PRIVACY_POLICY, MARKETING_EMAILS, DATA_PROCESSING';

COMMENT ON COLUMN user_consents.version IS
    'Version of the policy document the user consented to or revoked consent for.';
