-- Add missing columns to assessments table for job consumer
ALTER TABLE assessments
  ADD COLUMN started_at TIMESTAMP NULL,
  ADD COLUMN error_message TEXT NULL,
  ADD COLUMN result_json JSON NULL;

-- Create indexes for job queue operations
CREATE INDEX idx_assessments_created_at ON assessments(created_at DESC);
CREATE INDEX idx_assessments_started_at ON assessments(started_at);

-- Create table for BRF version tracking
CREATE TABLE brf_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(50) NOT NULL UNIQUE,
    git_tag VARCHAR(100),
    commit_hash VARCHAR(40),
    artifact_url TEXT,
    yaml_content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_brf_versions_version ON brf_versions(version);
CREATE INDEX idx_brf_versions_created_at ON brf_versions(created_at DESC);
