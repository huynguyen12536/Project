-- Test Database Initialization Script
-- Runs when PostgreSQL testcontainer starts
--
-- Purpose: Setup initial schema and test data for integration tests
-- Note: Flyway migrations will run after this script

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create assessment schema (if not exists)
-- Note: Actual schema created by Flyway migrations
-- This script ensures test DB is ready before migrations run

-- Create test users (optional - for reference)
-- These may be overridden by individual @BeforeEach fixtures

-- Create initial test data (optional)
-- Most tests prefer to create their own data in @BeforeEach

-- Verify schema exists after Flyway runs
-- This script runs BEFORE Flyway, so actual table creation happens there

COMMIT;
