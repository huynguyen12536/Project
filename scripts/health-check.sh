#!/usr/bin/env bash
# ============================================================================
# health-check.sh - HTTP health check with exponential backoff
# Usage: ./scripts/health-check.sh [OPTIONS]
#
# Options:
#   --url URL        Health check URL (required)
#   --timeout SECS   Max wait time in seconds (default: 120)
#   --interval SECS  Initial retry interval (default: 5)
#   --expected CODE  Expected HTTP status code (default: 200)
#
# Exit codes:
#   0  - Healthy (got expected HTTP status within timeout)
#   1  - Timeout or unexpected response
# ============================================================================

set -euo pipefail

# ---- Defaults ----
URL=""
TIMEOUT=120
INITIAL_INTERVAL=5
EXPECTED_CODE=200

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] [HEALTH] $*"; }
ok()   { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] ${GREEN}[OK]${NC} $*"; }
err()  { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] ${RED}[ERROR]${NC} $*" >&2; }
warn() { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] ${YELLOW}[WARN]${NC} $*"; }

# ---- Parse arguments ----
while [[ $# -gt 0 ]]; do
  case "$1" in
    --url)       URL="$2"; shift 2 ;;
    --timeout)   TIMEOUT="$2"; shift 2 ;;
    --interval)  INITIAL_INTERVAL="$2"; shift 2 ;;
    --expected)  EXPECTED_CODE="$2"; shift 2 ;;
    --help|-h)
      grep '^#' "$0" | cut -c3-
      exit 0
      ;;
    *) err "Unknown option: $1"; exit 1 ;;
  esac
done

if [ -z "$URL" ]; then
  err "URL is required. Use --url <endpoint>"
  exit 1
fi

# ============================================================================
# HEALTH CHECK LOOP with exponential backoff
# ============================================================================

log "Starting health check: $URL"
log "Timeout: ${TIMEOUT}s | Initial interval: ${INITIAL_INTERVAL}s | Expected HTTP: $EXPECTED_CODE"

START_TIME=$(date +%s)
DEADLINE=$((START_TIME + TIMEOUT))
ATTEMPT=0
INTERVAL=$INITIAL_INTERVAL
MAX_INTERVAL=30

while [ "$(date +%s)" -lt "$DEADLINE" ]; do
  ATTEMPT=$((ATTEMPT + 1))
  ELAPSED=$(( $(date +%s) - START_TIME ))
  REMAINING=$((DEADLINE - $(date +%s)))

  log "Attempt #${ATTEMPT} (elapsed: ${ELAPSED}s, remaining: ${REMAINING}s)"

  # Perform the HTTP check
  HTTP_CODE=$(curl \
    --silent \
    --output /dev/null \
    --write-out "%{http_code}" \
    --connect-timeout 5 \
    --max-time 10 \
    --retry 0 \
    "$URL" 2>/dev/null || echo "000")

  if [ "$HTTP_CODE" = "$EXPECTED_CODE" ]; then
    ELAPSED=$(( $(date +%s) - START_TIME ))
    ok "Health check PASSED (HTTP $HTTP_CODE) after ${ELAPSED}s (${ATTEMPT} attempts)"
    exit 0
  fi

  warn "HTTP $HTTP_CODE (expected $EXPECTED_CODE) — retrying in ${INTERVAL}s..."

  # Sleep up to deadline
  REMAINING=$((DEADLINE - $(date +%s)))
  if [ "$REMAINING" -le 0 ]; then
    break
  fi

  SLEEP_TIME=$((INTERVAL < REMAINING ? INTERVAL : REMAINING))
  sleep "$SLEEP_TIME"

  # Exponential backoff: double interval up to max
  INTERVAL=$((INTERVAL * 2))
  if [ "$INTERVAL" -gt "$MAX_INTERVAL" ]; then
    INTERVAL=$MAX_INTERVAL
  fi
done

ELAPSED=$(( $(date +%s) - START_TIME ))
err "Health check TIMED OUT after ${ELAPSED}s (${ATTEMPT} attempts)"
err "URL: $URL"
err "Last HTTP code received: $HTTP_CODE"
exit 1
