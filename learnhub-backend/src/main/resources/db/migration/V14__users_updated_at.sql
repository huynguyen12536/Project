-- V14: Align users table with User entity timestamps

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE users
SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

ALTER TABLE users
    ALTER COLUMN updated_at SET NOT NULL;

COMMENT ON COLUMN users.updated_at IS
    'Last update timestamp maintained by the application User entity.';
