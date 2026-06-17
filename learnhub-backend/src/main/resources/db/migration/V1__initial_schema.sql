CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash TEXT,
  role VARCHAR(50) DEFAULT 'LEARNER',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE user_oauth (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  provider VARCHAR(50) NOT NULL,
  github_login VARCHAR(255),
  token_meta JSONB,
  last_auth_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE rsa_keys (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  key_version INTEGER NOT NULL,
  public_pem TEXT NOT NULL,
  private_encrypted BYTEA NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE brf_versions (
  version VARCHAR(32) PRIMARY KEY,
  git_tag VARCHAR(128),
  commit_hash VARCHAR(128),
  released_at TIMESTAMP WITH TIME ZONE,
  description TEXT
);

INSERT INTO brf_versions (version, git_tag, commit_hash, released_at, description)
VALUES ('1.0.0', 'brf-1.0.0', 'HEAD', now(), 'Initial BRF v1.0')
ON CONFLICT (version) DO NOTHING;

CREATE TABLE repositories (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  github_repo_id BIGINT,
  full_name VARCHAR(512),
  private_flag BOOLEAN DEFAULT FALSE,
  snapshot_ref TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE assessments (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  repo_id UUID REFERENCES repositories(id) ON DELETE CASCADE,
  brf_version VARCHAR(32) REFERENCES brf_versions(version),
  submitted_by UUID REFERENCES users(id),
  submitted_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  status VARCHAR(32) DEFAULT 'QUEUED',
  result_summary JSONB
);

CREATE TABLE evidence_snapshots (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  assessment_id UUID REFERENCES assessments(id) ON DELETE CASCADE,
  storage_ref TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE competency_results (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  assessment_id UUID REFERENCES assessments(id) ON DELETE CASCADE,
  competency_key VARCHAR(128),
  score INTEGER,
  band VARCHAR(32),
  evidence JSONB
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
CREATE INDEX idx_assessments_submitted_at ON assessments(submitted_at);
