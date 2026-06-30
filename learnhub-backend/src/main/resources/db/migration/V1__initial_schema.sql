-- V1: Initial Schema
-- Establishes core tables for users, OAuth, RSA keys, repositories, refresh tokens, and access control.
-- NOTE: brf_versions is defined in V6 (canonical version with full schema).
-- NOTE: assessments is defined in V5 (canonical version with correct schema).
-- NOTE: user_oauth is superseded by V3 which adds the full OAuth column set.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash TEXT,
  role VARCHAR(50) DEFAULT 'LEARNER',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE rsa_keys (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  key_version INTEGER NOT NULL,
  public_pem TEXT NOT NULL,
  private_encrypted BYTEA NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE repositories (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  github_repo_id BIGINT,
  full_name VARCHAR(512),
  private_flag BOOLEAN DEFAULT FALSE,
  snapshot_ref TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL,
  issued_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  expires_at TIMESTAMP WITH TIME ZONE,
  revoked BOOLEAN DEFAULT FALSE
);

CREATE TABLE casbin_policy (
  id SERIAL PRIMARY KEY,
  p_type VARCHAR(100),
  v0 TEXT,
  v1 TEXT,
  v2 TEXT,
  v3 TEXT,
  v4 TEXT,
  v5 TEXT
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_repositories_github_id ON repositories(github_repo_id);
