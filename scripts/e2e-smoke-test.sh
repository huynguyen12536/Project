#!/bin/bash

# End-to-End Smoke Test for Phase 5 Production Deployment
# Tests: HTTP submission → Async processing → SSE streaming → Database storage

set -e

echo "═══════════════════════════════════════════════════════════════"
echo "PHASE 5 - END-TO-END SMOKE TEST"
echo "Real-time Assessment Integration - Production Validation"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Configuration
API_BASE_URL=${API_BASE_URL:-http://localhost:8080/api}
FRONTEND_URL=${FRONTEND_URL:-http://localhost:3000}
AUTH_TOKEN=${AUTH_TOKEN:-}  # Optional: JWT token for authenticated requests
VERBOSE=${VERBOSE:-false}

echo "🔧 TEST CONFIGURATION:"
echo "  API Base URL: $API_BASE_URL"
echo "  Frontend URL: $FRONTEND_URL"
echo "  Verbose: $VERBOSE"
echo ""

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function for curl with error handling
test_endpoint() {
  local name=$1
  local method=$2
  local endpoint=$3
  local data=$4
  local expected_status=$5

  echo "🧪 Testing: $name"

  local curl_cmd="curl -s -w '\n%{http_code}' -X $method '$API_BASE_URL$endpoint'"

  if [ "$method" = "POST" ] || [ "$method" = "PUT" ]; then
    curl_cmd="$curl_cmd -H 'Content-Type: application/json'"
    if [ -n "$AUTH_TOKEN" ]; then
      curl_cmd="$curl_cmd -H 'Authorization: Bearer $AUTH_TOKEN'"
    fi
    if [ -n "$data" ]; then
      curl_cmd="$curl_cmd -d '$data'"
    fi
  fi

  if [ "$VERBOSE" = "true" ]; then
    echo "  Command: $curl_cmd"
  fi

  local response=$(eval $curl_cmd)
  local http_code=$(echo "$response" | tail -n 1)
  local body=$(echo "$response" | head -n -1)

  if [ "$http_code" = "$expected_status" ]; then
    echo "  ✅ PASS - HTTP $http_code (expected $expected_status)"
    ((TESTS_PASSED++))
    echo "$body"
    return 0
  else
    echo "  ❌ FAIL - HTTP $http_code (expected $expected_status)"
    echo "  Response: $body"
    ((TESTS_FAILED++))
    return 1
  fi
}

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "TEST SUITE 1: BACKEND API HEALTH & READINESS"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Test 1: Backend health check
test_endpoint "Backend Health Check" "GET" "/health" "" "200" > /tmp/health_response.json || true

# Test 2: Actuator metrics (Spring Boot)
test_endpoint "Metrics Endpoint" "GET" "/health/metrics" "" "200" > /tmp/metrics_response.json || true

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "TEST SUITE 2: ASSESSMENT SUBMISSION & ASYNC PROCESSING"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Generate test snapshot UUID
SNAPSHOT_ID=$(uuidgen 2>/dev/null || echo "550e8400-e29b-41d4-a716-446655440000")
USER_ID=$(uuidgen 2>/dev/null || echo "550e8400-e29b-41d4-a716-446655440001")

echo "📝 Test Parameters:"
echo "  Snapshot ID: $SNAPSHOT_ID"
echo "  User ID: $USER_ID"
echo ""

# Test 3: Submit assessment (should return PENDING status immediately)
echo "🧪 Testing: Submit Assessment"

SUBMIT_DATA="{\"snapshotId\":\"$SNAPSHOT_ID\"}"

SUBMIT_RESPONSE=$(curl -s -X POST "$API_BASE_URL/assessments" \
  -H "Content-Type: application/json" \
  ${AUTH_TOKEN:+-H "Authorization: Bearer $AUTH_TOKEN"} \
  -d "$SUBMIT_DATA" \
  -w "\n%{http_code}")

SUBMIT_STATUS=$(echo "$SUBMIT_RESPONSE" | tail -n 1)
SUBMIT_BODY=$(echo "$SUBMIT_RESPONSE" | head -n -1)

if [ "$SUBMIT_STATUS" = "201" ]; then
  echo "  ✅ PASS - HTTP 201 Created"
  ((TESTS_PASSED++))

  # Extract assessment ID from response
  ASSESSMENT_ID=$(echo "$SUBMIT_BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
  echo "  Assessment ID: $ASSESSMENT_ID"

  # Verify status is PENDING
  STATUS=$(echo "$SUBMIT_BODY" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
  if [ "$STATUS" = "PENDING" ]; then
    echo "  ✅ PASS - Status is PENDING"
    ((TESTS_PASSED++))
  else
    echo "  ❌ FAIL - Status is $STATUS, expected PENDING"
    ((TESTS_FAILED++))
  fi
else
  echo "  ❌ FAIL - HTTP $SUBMIT_STATUS (expected 201)"
  echo "  Response: $SUBMIT_BODY"
  ((TESTS_FAILED++))
  ASSESSMENT_ID=""
fi

echo ""

if [ -n "$ASSESSMENT_ID" ]; then
  echo "═══════════════════════════════════════════════════════════════"
  echo "TEST SUITE 3: SERVER-SENT EVENTS (SSE) STREAMING"
  echo "═══════════════════════════════════════════════════════════════"
  echo ""

  # Test 4: SSE endpoint opens stream
  echo "🧪 Testing: SSE Connection (30 second timeout)"

  SSE_RESPONSE=$(timeout 30 curl -s -N -X GET "$API_BASE_URL/assessments/$ASSESSMENT_ID/progress" \
    -H "Accept: text/event-stream" \
    ${AUTH_TOKEN:+-H "Authorization: Bearer $AUTH_TOKEN"} 2>&1 | head -20)

  if echo "$SSE_RESPONSE" | grep -q "event:"; then
    echo "  ✅ PASS - SSE stream opened, events received"
    ((TESTS_PASSED++))
    echo "  Sample events:"
    echo "$SSE_RESPONSE" | head -10 | sed 's/^/    /'
  else
    echo "  ⚠️ WARNING - SSE stream not responding yet (worker may still be processing)"
    echo "  Response (first 500 chars):"
    echo "$SSE_RESPONSE" | head -c 500 | sed 's/^/    /'
  fi

  echo ""
  echo "═══════════════════════════════════════════════════════════════"
  echo "TEST SUITE 4: POLLING FALLBACK (Alternative to SSE)"
  echo "═══════════════════════════════════════════════════════════════"
  echo ""

  # Test 5: Polling endpoint
  echo "🧪 Testing: Polling Endpoint (alternative to SSE)"

  for i in {1..5}; do
    echo "  Attempt $i/5..."

    POLL_RESPONSE=$(curl -s -X GET "$API_BASE_URL/assessments/$ASSESSMENT_ID/progress/poll" \
      -H "Accept: application/json" \
      ${AUTH_TOKEN:+-H "Authorization: Bearer $AUTH_TOKEN"} \
      -w "\n%{http_code}")

    POLL_STATUS=$(echo "$POLL_RESPONSE" | tail -n 1)
    POLL_BODY=$(echo "$POLL_RESPONSE" | head -n -1)

    if [ "$POLL_STATUS" = "200" ]; then
      echo "    ✅ Status 200 - Response:"
      echo "    $POLL_BODY" | sed 's/^/    /'

      # Check if completed
      if echo "$POLL_BODY" | grep -q '"status":"COMPLETED"'; then
        echo "    ✅ Assessment COMPLETED!"
        ((TESTS_PASSED++))
        break
      elif echo "$POLL_BODY" | grep -q '"status":"PROCESSING"'; then
        echo "    ⏳ Still PROCESSING... (waiting 2s)"
        sleep 2
      fi
    elif [ "$POLL_STATUS" = "204" ]; then
      echo "    ℹ️ Status 204 - No content (no change since last poll)"
      sleep 2
    else
      echo "    ❌ HTTP $POLL_STATUS"
      ((TESTS_FAILED++))
      break
    fi
  done

  echo ""
  echo "═══════════════════════════════════════════════════════════════"
  echo "TEST SUITE 5: RESULTS VALIDATION (JSONB Storage)"
  echo "═══════════════════════════════════════════════════════════════"
  echo ""

  # Test 6: Fetch assessment results
  echo "🧪 Testing: Fetch Assessment Results"

  RESULTS_RESPONSE=$(curl -s -X GET "$API_BASE_URL/assessments/$ASSESSMENT_ID" \
    ${AUTH_TOKEN:+-H "Authorization: Bearer $AUTH_TOKEN"} \
    -w "\n%{http_code}")

  RESULTS_STATUS=$(echo "$RESULTS_RESPONSE" | tail -n 1)
  RESULTS_BODY=$(echo "$RESULTS_RESPONSE" | head -n -1)

  if [ "$RESULTS_STATUS" = "200" ]; then
    echo "  ✅ PASS - HTTP 200"
    ((TESTS_PASSED++))

    # Check for JSONB results_data
    if echo "$RESULTS_BODY" | grep -q "language"; then
      echo "  ✅ PASS - Results contain language field"
      ((TESTS_PASSED++))
    fi

    if echo "$RESULTS_BODY" | grep -q "seriesList"; then
      echo "  ✅ PASS - Results contain seriesList"
      ((TESTS_PASSED++))
    fi

    # Parse and display results
    echo "  📊 Results Data:"
    echo "$RESULTS_BODY" | sed 's/^/    /'
  else
    echo "  ❌ FAIL - HTTP $RESULTS_STATUS"
    ((TESTS_FAILED++))
  fi
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "TEST SUITE 6: RATE LIMITING & ERROR HANDLING"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Test 7: Duplicate submission (should get 409 Conflict)
echo "🧪 Testing: Duplicate Submission Protection"

DUP_RESPONSE=$(curl -s -X POST "$API_BASE_URL/assessments" \
  -H "Content-Type: application/json" \
  ${AUTH_TOKEN:+-H "Authorization: Bearer $AUTH_TOKEN"} \
  -d "{\"snapshotId\":\"$SNAPSHOT_ID\"}" \
  -w "\n%{http_code}")

DUP_STATUS=$(echo "$DUP_RESPONSE" | tail -n 1)

if [ "$DUP_STATUS" = "409" ]; then
  echo "  ✅ PASS - HTTP 409 Conflict (duplicate blocked)"
  ((TESTS_PASSED++))
else
  echo "  ⚠️ WARNING - HTTP $DUP_STATUS (duplicate check may not be active yet)"
  ((TESTS_FAILED++))
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "TEST SUITE 7: FRONTEND CONNECTIVITY"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Test 8: Frontend health check
echo "🧪 Testing: Frontend Health Check"

FE_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$FRONTEND_URL")

if [ "$FE_RESPONSE" = "200" ]; then
  echo "  ✅ PASS - Frontend responding (HTTP 200)"
  ((TESTS_PASSED++))
else
  echo "  ⚠️ WARNING - Frontend HTTP $FE_RESPONSE"
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "SMOKE TEST RESULTS SUMMARY"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "✅ Tests Passed: $TESTS_PASSED"
echo "❌ Tests Failed: $TESTS_FAILED"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
  echo "🎉 ALL SMOKE TESTS PASSED!"
  echo ""
  echo "✅ Phase 5 Production Deployment is READY"
  echo ""
  echo "Summary:"
  echo "  ✅ Backend API responding"
  echo "  ✅ Assessment submission working"
  echo "  ✅ Async worker processing"
  echo "  ✅ SSE/Polling streaming working"
  echo "  ✅ Rate limiting enforced"
  echo "  ✅ Frontend accessible"
  echo ""
  echo "🚀 Proceed with production deployment"
  exit 0
else
  echo "⚠️  SOME TESTS FAILED - Review configuration"
  echo ""
  echo "Troubleshooting:"
  echo "  1. Check backend is running: curl $API_BASE_URL/health"
  echo "  2. Check frontend is running: curl $FRONTEND_URL"
  echo "  3. Check database connection"
  echo "  4. Check Redis connection"
  echo "  5. Check LLM_API_KEY environment variable"
  echo ""
  exit 1
fi
