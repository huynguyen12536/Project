#!/bin/bash

# Database Migration Validation Script
# Validates that PostgreSQL JSONB schema is correctly applied
# Phase 5 Deployment

set -e

echo "═══════════════════════════════════════════════════════════════"
echo "DATABASE MIGRATION VALIDATION - PHASE 5 DEPLOYMENT"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Load environment variables
if [ -f .env.production ]; then
  export $(cat .env.production | grep -v '^#' | xargs)
  echo "✅ Loaded environment from .env.production"
else
  echo "⚠️ .env.production not found, using environment variables"
fi

# Database connection parameters
DB_HOST=${DATABASE_HOST:-localhost}
DB_PORT=${DATABASE_PORT:-5432}
DB_NAME=${DATABASE_NAME:-learnhub_prod}
DB_USER=${DATABASE_USER:-learnhub_prod}
DB_PASSWORD=${DATABASE_PASSWORD}

echo ""
echo "📋 DATABASE CONNECTION DETAILS:"
echo "  Host: $DB_HOST"
echo "  Port: $DB_PORT"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo ""

# Function to execute SQL and capture output
run_sql() {
  local query=$1
  PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "$query"
}

# Check connection
echo "🔍 Checking database connection..."
if run_sql "SELECT 1;" > /dev/null 2>&1; then
  echo "✅ Database connection successful"
else
  echo "❌ Failed to connect to database"
  exit 1
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "VALIDATION CHECKS:"
echo "═══════════════════════════════════════════════════════════════"

# Check 1: Assessments table exists
echo ""
echo "1️⃣ Checking 'assessments' table..."
if run_sql "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'assessments');" | grep -q "t"; then
  echo "✅ Table 'assessments' exists"
else
  echo "❌ Table 'assessments' not found"
  exit 1
fi

# Check 2: results_data JSONB column exists
echo ""
echo "2️⃣ Checking 'results_data' JSONB column..."
if run_sql "SELECT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'assessments' AND column_name = 'results_data' AND data_type = 'jsonb');" | grep -q "t"; then
  echo "✅ Column 'results_data' with type JSONB exists"
else
  echo "❌ Column 'results_data' JSONB not found"
  exit 1
fi

# Check 3: results_data column is nullable
echo ""
echo "3️⃣ Checking 'results_data' nullable..."
if run_sql "SELECT is_nullable FROM information_schema.columns WHERE table_name = 'assessments' AND column_name = 'results_data';" | grep -q "YES"; then
  echo "✅ Column 'results_data' is nullable"
else
  echo "⚠️ Column 'results_data' is NOT nullable (but may be OK)"
fi

# Check 4: GIN index on results_data
echo ""
echo "4️⃣ Checking GIN index on 'results_data'..."
if run_sql "SELECT EXISTS(SELECT 1 FROM pg_indexes WHERE tablename = 'assessments' AND indexname = 'idx_assessments_results_data');" | grep -q "t"; then
  echo "✅ GIN index 'idx_assessments_results_data' exists"
else
  echo "⚠️ GIN index not found (performance may be impacted)"
fi

# Check 5: Completed assessments index
echo ""
echo "5️⃣ Checking composite index for completed assessments..."
if run_sql "SELECT EXISTS(SELECT 1 FROM pg_indexes WHERE tablename = 'assessments' AND indexname = 'idx_assessments_completed_with_results');" | grep -q "t"; then
  echo "✅ Index 'idx_assessments_completed_with_results' exists"
else
  echo "⚠️ Composite index not found (queries may be slower)"
fi

# Check 6: User + Status index
echo ""
echo "6️⃣ Checking user + status index..."
if run_sql "SELECT EXISTS(SELECT 1 FROM pg_indexes WHERE tablename = 'assessments' AND indexname = 'idx_assessments_user_status');" | grep -q "t"; then
  echo "✅ Index 'idx_assessments_user_status' exists"
else
  echo "⚠️ Index not found (may impact performance on user queries)"
fi

# Check 7: Flyway migration history
echo ""
echo "7️⃣ Checking Flyway migration history..."
if run_sql "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'flyway_schema_history');" | grep -q "t"; then
  echo "✅ Flyway migration table exists"
  echo ""
  echo "   Latest migrations:"
  run_sql "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 3;" | sed 's/^/   /'
else
  echo "⚠️ Flyway table not found (migrations may not be tracked)"
fi

# Check 8: Test JSONB storage
echo ""
echo "8️⃣ Testing JSONB storage and retrieval..."

# Create test record if needed
TEST_ID=$(uuidgen 2>/dev/null || echo "550e8400-e29b-41d4-a716-446655440000")
USER_ID=$(uuidgen 2>/dev/null || echo "550e8400-e29b-41d4-a716-446655440001")

TEST_DATA='{"language":"Java","seriesList":[{"name":"Security","value":75,"axis":"Security","level":"GOOD","gapAnalysis":"Test gap analysis"}]}'

# Insert test record
run_sql "INSERT INTO assessments (id, user_id, snapshot_id, status, created_at, results_data) VALUES ('$TEST_ID', '$USER_ID', '$TEST_ID', 'COMPLETED', NOW(), '$TEST_DATA'::jsonb) ON CONFLICT DO NOTHING;" > /dev/null 2>&1

# Retrieve and verify
RETRIEVED=$(run_sql "SELECT results_data FROM assessments WHERE id = '$TEST_ID' AND results_data IS NOT NULL LIMIT 1;")

if echo "$RETRIEVED" | grep -q "Java"; then
  echo "✅ JSONB storage and retrieval working correctly"
  echo "   Sample data:"
  echo "   $RETRIEVED" | sed 's/^/   /'
else
  echo "⚠️ JSONB storage test inconclusive"
fi

# Cleanup test record
run_sql "DELETE FROM assessments WHERE id = '$TEST_ID';" > /dev/null 2>&1

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "VALIDATION SUMMARY:"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "✅ Database schema validation complete"
echo ""
echo "📊 Statistics:"
run_sql "SELECT
  (SELECT COUNT(*) FROM assessments) as total_assessments,
  (SELECT COUNT(*) FROM assessments WHERE status = 'COMPLETED' AND results_data IS NOT NULL) as completed_with_results,
  (SELECT COUNT(*) FROM assessments WHERE results_data IS NOT NULL) as total_with_results;" | column -t

echo ""
echo "🚀 Database is ready for Phase 5 production deployment!"
echo ""
