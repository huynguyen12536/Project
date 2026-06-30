#!/usr/bin/env bash
# ============================================================================
# rollback.sh - Emergency rollback to previous deployment color
# Usage: ./scripts/rollback.sh [OPTIONS]
#
# Options:
#   --env ENV        Environment: staging|production (default: staging)
#   --force          Skip confirmation prompt
#   --dry-run        Print actions without executing
#
# This script reads the current color from .deploy-state and switches
# Nginx back to the PREVIOUS color, then stops the broken new container.
# ============================================================================

set -euo pipefail

APP_DIR="${APP_DIR:-/opt/learnhub}"
STATE_FILE="${APP_DIR}/.deploy-state"
ROLLBACK_STATE_FILE="${APP_DIR}/.rollback-state"
DEPLOY_ENV="${DEPLOY_ENV:-staging}"
FORCE=false
DRY_RUN=false

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log()  { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] ${BLUE}[ROLLBACK]${NC} $*"; }
ok()   { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] ${GREEN}[OK]${NC} $*"; }
warn() { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] ${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] ${RED}[ERROR]${NC} $*" >&2; }

# ---- Parse arguments ----
while [[ $# -gt 0 ]]; do
  case "$1" in
    --env)      DEPLOY_ENV="$2"; shift 2 ;;
    --force)    FORCE=true; shift ;;
    --dry-run)  DRY_RUN=true; shift ;;
    --help|-h)
      grep '^#' "$0" | cut -c3-
      exit 0
      ;;
    *) err "Unknown option: $1"; exit 1 ;;
  esac
done

run_cmd() {
  if [ "$DRY_RUN" = true ]; then
    echo "[DRY-RUN] $*"
  else
    eval "$@"
  fi
}

# ---- Determine rollback target ----
if [ ! -f "$STATE_FILE" ]; then
  err "No deployment state file found at $STATE_FILE"
  err "Cannot determine current/previous color for rollback"
  exit 1
fi

CURRENT_COLOR=$(cat "$STATE_FILE")
if [ "$CURRENT_COLOR" = "blue" ]; then
  ROLLBACK_COLOR="green"
else
  ROLLBACK_COLOR="blue"
fi

# ---- Confirm rollback ----
log "=============================================="
log " ROLLBACK OPERATION"
log " Environment    : $DEPLOY_ENV"
log " Current color  : $CURRENT_COLOR (BROKEN)"
log " Rollback target: $ROLLBACK_COLOR (PREVIOUS)"
log " Dry run        : $DRY_RUN"
log "=============================================="

if [ "$FORCE" = false ] && [ "$DRY_RUN" = false ]; then
  echo ""
  read -r -p "Are you sure you want to rollback? (yes/no): " CONFIRM
  if [ "$CONFIRM" != "yes" ]; then
    log "Rollback cancelled by user"
    exit 0
  fi
fi

# ---- Check that rollback target exists ----
ROLLBACK_BACKEND="learnhub-backend-${ROLLBACK_COLOR}"
ROLLBACK_FRONTEND="learnhub-frontend-${ROLLBACK_COLOR}"

BACKEND_RUNNING=$(docker ps --format '{{.Names}}' | grep -c "^${ROLLBACK_BACKEND}$" || true)
if [ "$BACKEND_RUNNING" -eq 0 ]; then
  err "Rollback target container $ROLLBACK_BACKEND is NOT running"
  err "Cannot rollback — previous version is not available"
  err "You may need to redeploy the previous image tag manually"
  exit 1
fi

ok "Rollback target $ROLLBACK_BACKEND is running — proceeding"

# ---- Step 1: Switch Nginx back to rollback color ----
log "Step 1: Switching Nginx to ${ROLLBACK_COLOR} containers..."
NGINX_CONF="${APP_DIR}/nginx/conf.d/learnhub.conf"
NGINX_TMP="${NGINX_CONF}.tmp"

run_cmd "sed \
  -e 's|server learnhub-backend-[a-z]*:[0-9]*|server ${ROLLBACK_BACKEND}:8080|g' \
  -e 's|server backend:[0-9]*|server ${ROLLBACK_BACKEND}:8080|g' \
  -e 's|server learnhub-frontend-[a-z]*:[0-9]*|server ${ROLLBACK_FRONTEND}:3000|g' \
  -e 's|server frontend:[0-9]*|server ${ROLLBACK_FRONTEND}:3000|g' \
  '$NGINX_CONF' > '$NGINX_TMP' && mv '$NGINX_TMP' '$NGINX_CONF'"

if docker ps --format '{{.Names}}' | grep -q "learnhub-nginx"; then
  run_cmd "docker exec learnhub-nginx nginx -t && docker exec learnhub-nginx nginx -s reload"
  ok "Nginx reloaded — traffic now going to ${ROLLBACK_COLOR}"
else
  warn "Nginx not running — config updated but no reload performed"
fi

# ---- Step 2: Update state file ----
run_cmd "echo '${ROLLBACK_COLOR}' > '$STATE_FILE'"
log "State updated: ${CURRENT_COLOR} -> ${ROLLBACK_COLOR}"

# ---- Step 3: Stop broken containers ----
BROKEN_BACKEND="learnhub-backend-${CURRENT_COLOR}"
BROKEN_FRONTEND="learnhub-frontend-${CURRENT_COLOR}"

log "Step 3: Stopping broken ${CURRENT_COLOR} containers..."
for cname in "$BROKEN_BACKEND" "$BROKEN_FRONTEND"; do
  if docker ps -a --format '{{.Names}}' | grep -q "^${cname}$"; then
    run_cmd "docker stop '$cname' && docker rm '$cname'"
    ok "Removed: $cname"
  else
    warn "Container not found: $cname (may already be stopped)"
  fi
done

# ---- Step 4: Record rollback event ----
ROLLBACK_TIME=$(date -u +%Y-%m-%dT%H:%M:%SZ)
run_cmd "echo '{\"time\":\"${ROLLBACK_TIME}\",\"from\":\"${CURRENT_COLOR}\",\"to\":\"${ROLLBACK_COLOR}\",\"env\":\"${DEPLOY_ENV}\"}' >> '$ROLLBACK_STATE_FILE'"

# ---- Step 5: Notify ops team ----
if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
  run_cmd "curl -s -X POST '$SLACK_WEBHOOK_URL' \
    -H 'Content-type: application/json' \
    -d '{
      \"text\": \":warning: *LearnHub ROLLBACK executed* — reverted ${CURRENT_COLOR} -> ${ROLLBACK_COLOR} | Env: ${DEPLOY_ENV} | Time: ${ROLLBACK_TIME}\"
    }'" || warn "Slack notification failed (non-critical)"
fi

ok "=============================================="
ok " ROLLBACK COMPLETE"
ok " Active version  : $ROLLBACK_COLOR"
ok " Removed version : $CURRENT_COLOR"
ok " Environment     : $DEPLOY_ENV"
ok " Time            : $(date -u +%Y-%m-%dT%H:%M:%SZ)"
ok "=============================================="
