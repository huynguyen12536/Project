-- V9: Assessments Schema Consolidation
-- ============================================================================
-- CONTEXT:
-- The original V1 schema defined an early 'assessments' table with columns:
--   repo_id, brf_version (FK to brf_versions.version), submitted_by, submitted_at, status, result_summary
-- It also defined dependent tables: evidence_snapshots, competency_results.
-- V5 redefined 'assessments' with the correct Phase 2/3 schema:
--   user_id, snapshot_id, status, created_at, completed_at
--
-- RESOLUTION (applied in V1 cleanup):
-- - V1 no longer creates assessments, evidence_snapshots, competency_results, brf_versions, or user_oauth.
-- - V3 creates user_oauth (full schema with token hashes and scopes).
-- - V5 creates assessments (canonical schema referencing repository_snapshots).
-- - V6 creates brf_versions (full schema with artifact tracking) and adds job-tracking columns to assessments.
-- - V7 creates assessment_results.
-- - V8.5 adds results_data JSONB column to assessments.
--
-- This migration adds the legacy-compat columns for any code that still references
-- the old assessment structure, using nullable ALTER TABLE ADD COLUMN IF NOT EXISTS.
-- ============================================================================

-- Add repo_id as nullable column for backward compat (old code may reference it)
ALTER TABLE assessments
  ADD COLUMN IF NOT EXISTS repo_id UUID REFERENCES repositories(id) ON DELETE SET NULL;

-- Add submitted_by as nullable (maps to user_id semantically, kept for legacy API compat)
ALTER TABLE assessments
  ADD COLUMN IF NOT EXISTS submitted_by UUID REFERENCES users(id) ON DELETE SET NULL;

-- Add submitted_at for legacy compat (maps to created_at semantically)
ALTER TABLE assessments
  ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP WITH TIME ZONE DEFAULT now();

-- Add result_summary JSONB for legacy compat
ALTER TABLE assessments
  ADD COLUMN IF NOT EXISTS result_summary JSONB;

-- Index for backward-compat queries by repo_id
CREATE INDEX IF NOT EXISTS idx_assessments_repo_id
  ON assessments(repo_id)
  WHERE repo_id IS NOT NULL;

-- Index for submitted_at queries (legacy pattern)
CREATE INDEX IF NOT EXISTS idx_assessments_submitted_at
  ON assessments(submitted_at DESC);
