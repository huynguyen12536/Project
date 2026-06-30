-- V13: Email verification OTP attempt tracking
-- Adds a bounded attempt counter for OTP-based account activation.

ALTER TABLE email_verification_tokens
    ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN email_verification_tokens.attempt_count IS
    'Number of failed verification attempts for the active OTP/token.';
