-- Phase 4: Assessment Results Storage Schema
-- Adds JSONB support for RadarSeriesDto results from hybrid evaluation engine

-- Step 1: Add results_data column (JSONB) to assessments table
-- Stores RadarSeriesDto with schema: {language, seriesList: [{name, value, axis, level, gapAnalysis}]}
ALTER TABLE assessments
ADD COLUMN results_data JSONB NULL;

-- Step 2: Create index on results_data for efficient queries
-- Supports queries like: WHERE results_data @> '{"language": "Java"}'
CREATE INDEX idx_assessments_results_data
ON assessments
USING GIN(results_data);

-- Step 3: Create partial index for completed assessments
-- Optimizes queries filtering by status = 'COMPLETED'
CREATE INDEX idx_assessments_completed_with_results
ON assessments (id, status, results_data)
WHERE status = 'COMPLETED' AND results_data IS NOT NULL;

-- Step 4: Add comment documenting the JSONB schema
COMMENT ON COLUMN assessments.results_data IS
'RadarSeriesDto stored as JSONB. Schema: {
  "language": "Java|Python|...",
  "seriesList": [
    {
      "name": "Security|Database|Architecture|Code Quality",
      "value": 0-100,
      "axis": "Security|Database|Architecture|Code Quality",
      "level": "EXCELLENT|GOOD|FAIR|POOR",
      "gapAnalysis": "Actionable text with specific findings and recommendations"
    }
  ]
}';

-- Step 5: Add completed_at column if not exists (may already exist from Phase 3)
-- Tracks when assessment processing completed
ALTER TABLE assessments
ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE NULL;

-- Step 6: Create index on userId + status for efficient queries
-- Supports: Find all PROCESSING assessments for a user
CREATE INDEX IF NOT EXISTS idx_assessments_user_status
ON assessments (user_id, status);

-- Step 7: Verify table structure
-- This is a read-only operation to document the final schema
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'assessments';
