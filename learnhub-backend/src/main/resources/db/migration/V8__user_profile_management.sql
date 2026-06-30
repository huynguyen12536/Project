-- V8__user_profile_management.sql
-- Add profile management fields to users table
-- Date: 2026-06-19

-- Add username column (unique, required for profile management)
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(50) UNIQUE;

-- Add avatar_url column for profile pictures
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

-- Update first_name and last_name columns to have length constraints
-- PostgreSQL syntax: ALTER COLUMN ... TYPE (not MySQL's MODIFY COLUMN)
ALTER TABLE users
  ALTER COLUMN first_name TYPE VARCHAR(50),
  ALTER COLUMN last_name TYPE VARCHAR(50);

-- Update bio column to have length constraint
ALTER TABLE users
  ALTER COLUMN bio TYPE VARCHAR(500);

-- Create index on username for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Create index on avatar_url for quick deletion/update
CREATE INDEX IF NOT EXISTS idx_users_avatar_url ON users(avatar_url);
