#!/usr/bin/env bash
# ============================================================================
# deploy-blue-green.sh - Zero-downtime Blue/Green Deployment
# Usage: ./scripts/deploy-blue-green.sh [OPTIONS]
#
# Options:
#   --image-tag TAG         Docker image tag to deploy (default: latest)
#   --backend-image IMAGE   Backend image name (default: learnhub-backend)
#   --frontend-image IMAGE  Frontend image name (default: learnhub-frontend)
#   --env ENV               Environment: dev|staging|production (default: staging)
#   --dry-run               Print actions without executing
#
# Environment variables (override CLI args):
#   NEW_IMAGE_TAG, BACKEND_IMAGE, FRONTEND_IMAGE
# ============================================================================

set -euo pipefail

# ---- Defaults ----
IMAGE_TAG="${NEW_IMAGE_TAG:-latest}"
BACKEND_IMAGE="${BACKEND_IMAGE:-learnhub-backend}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:-learnhub-frontend}"
DEPLOY_ENV="${DEPLOY_ENV:-staging}"
DRY_RUN=false
APP_DIR="${APP_DIR:-/opt/learnhub}"
HEALTH_TIMEOUT=120
DRAIN_SECONDS=30

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log()  { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] ${BLUE}[INFO]${NC} $*"; }
ok()   { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] ${GREEN}[OK]${NC} $*"; }
warn() { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] ${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] ${RED}[ERROR]${NC} $*" >&2; }

# ---- Parse arguments ----
while [[ $# -gt 0 ]]; do
  case "$1" in
    --image-tag)      IMAGE_TAG="$2"; shift 2 ;;
    --backend-image)  BACKEND_IMAGE="$2"; shift 2 ;;
    --frontend-image) FRONTEND_IMAGE="$2"; shift 2 ;;
    --env)            DEPLOY_ENV="$2"; shift 2 ;;
    --dry-run)        DRY_RUN=true; shift ;;
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

# ---- State file for current color ----
STATE_FILE="${APP_DIR}/.deploy-state"

get_current_color() {
  if [ -f "$STATE_FILE" ]; then
    cat "$STATE_FILE"
  else
    echo "blue"
  fi
}

set_current_color() {
  echo "$1" > "$STATE_FILE"
}

get_new_color() {
  local current="$1"
  if [ "$current" = "blue" ]; then
    echo "green"
  else
    echo "blue"
  fi
}

# ---- Container naming ----
container_name() {
  local service="$1" color="$2"
  echo "learnhub-${service}-${color}"
}

# ---- Pull new images ----
pull_images() {
  local backend_full="${BACKEND_IMAGE}:${IMAGE_TAG}"
  local frontend_full="${FRONTEND_IMAGE}:${IMAGE_TAG}"

  log "Pulling backend image: $backend_full"
  run_cmd "docker pull '$backend_full'"

  log "Pulling frontend image: $frontend_full"
  run_cmd "docker pull '$frontend_full'"
}

# ---- Start new containers ----
start_new_containers() {
  local new_color="$1"
  local backend_container
  local frontend_container
  backend_container=$(container_name "backend" "$new_color")
  frontend_container=$(container_name "frontend" "$new_color")

  log "Starting new ${new_color} backend container: $backend_container"
  run_cmd "docker run -d \
    --name '$backend_container' \
    --network learnhub-network \
    --env-file '${APP_DIR}/.env.${DEPLOY_ENV}' \
    --label 'learnhub.color=${new_color}' \
    --label 'learnhub.deploy.sha=${IMAGE_TAG}' \
    --label 'learnhub.deploy.time=$(date -u +%Y-%m-%dT%H:%M:%SZ)' \
    --health-cmd 'curl -f http://localhost:8080/actuator/health || exit 1' \
    --health-interval 10s \
    --health-timeout 5s \
    --health-retries 12 \
    --health-start-period 60s \
    --restart unless-stopped \
    '${BACKEND_IMAGE}:${IMAGE_TAG}'"

  log "Starting new ${new_color} frontend container: $frontend_container"
  run_cmd "docker run -d \
    --name '$frontend_container' \
    --network learnhub-network \
    --label 'learnhub.color=${new_color}' \
    --label 'learnhub.deploy.sha=${IMAGE_TAG}' \
    --restart unless-stopped \
    '${FRONTEND_IMAGE}:${IMAGE_TAG}'"
}

# ---- Wait for containers to be healthy ----
wait_for_healthy() {
  local new_color="$1"
  local backend_container
  backend_container=$(container_name "backend" "$new_color")
  local deadline=$(($(date +%s) + HEALTH_TIMEOUT))

  log "Waiting for $backend_container to be healthy (timeout: ${HEALTH_TIMEOUT}s)..."

  while [ "$(date +%s)" -lt "$deadline" ]; do
    local health
    health=$(docker inspect --format='{{.State.Health.Status}}' "$backend_container" 2>/dev/null || echo "not_found")
    case "$health" in
      healthy)
        ok "$backend_container is healthy"
        return 0
        ;;
      unhealthy)
        err "$backend_container reported unhealthy"
        docker logs --tail 50 "$backend_container" >&2
        return 1
        ;;
      starting)
        echo -n "."
        sleep 5
        ;;
      *)
        warn "Container health: $health — retrying..."
        sleep 5
        ;;
    esac
  done

  err "Health check timeout after ${HEALTH_TIMEOUT}s"
  return 1
}

# ---- Switch Nginx upstream ----
switch_nginx_upstream() {
  local new_color="$1"
  local backend_container
  local frontend_container
  backend_container=$(container_name "backend" "$new_color")
  frontend_container=$(container_name "frontend" "$new_color")

  log "Switching Nginx upstream to ${new_color} (${backend_container})"

  # Update the Nginx upstream config to point to new containers
  # by updating a symlink or templated conf file
  local nginx_conf="${APP_DIR}/nginx/conf.d/learnhub.conf"
  local nginx_tmp="${nginx_conf}.tmp"

  # Replace upstream server references
  run_cmd "sed \
    -e 's|server backend:[0-9]*|server ${backend_container}:8080|g' \
    -e 's|server frontend:[0-9]*|server ${frontend_container}:3000|g' \
    '$nginx_conf' > '$nginx_tmp' && mv '$nginx_tmp' '$nginx_conf'"

  # Reload Nginx gracefully (no downtime)
  if docker ps --format '{{.Names}}' | grep -q "learnhub-nginx"; then
    run_cmd "docker exec learnhub-nginx nginx -t && docker exec learnhub-nginx nginx -s reload"
    ok "Nginx reloaded pointing to ${new_color} containers"
  else
    warn "Nginx container not running — skipping reload"
  fi
}

# ---- Drain connections ----
drain_connections() {
  log "Draining connections from old version (${DRAIN_SECONDS}s)..."
  run_cmd "sleep $DRAIN_SECONDS"
  ok "Drain period complete"
}

# ---- Stop old containers ----
stop_old_containers() {
  local old_color="$1"
  local backend_container
  local frontend_container
  backend_container=$(container_name "backend" "$old_color")
  frontend_container=$(container_name "frontend" "$old_color")

  for cname in "$backend_container" "$frontend_container"; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${cname}$"; then
      log "Stopping old container: $cname"
      run_cmd "docker stop '$cname' && docker rm '$cname'"
      ok "Removed: $cname"
    else
      warn "Container not found (already gone?): $cname"
    fi
  done
}

# ---- Rollback ----
rollback() {
  local old_color="$1"
  local new_color="$2"

  err "Deployment FAILED — rolling back to ${old_color}"

  # Switch Nginx back to old color
  switch_nginx_upstream "$old_color"

  # Stop failed new containers
  local backend_container
  local frontend_container
  backend_container=$(container_name "backend" "$new_color")
  frontend_container=$(container_name "frontend" "$new_color")

  for cname in "$backend_container" "$frontend_container"; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${cname}$"; then
      warn "Removing failed container: $cname"
      run_cmd "docker stop '$cname' && docker rm '$cname'" || true
    fi
  done

  # Restore state file
  set_current_color "$old_color"
  err "Rollback complete. System is on ${old_color}."

  # Notify ops (if SLACK_WEBHOOK_URL is set)
  if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
    curl -s -X POST "$SLACK_WEBHOOK_URL" \
      -H 'Content-type: application/json' \
      -d "{\"text\":\":rotating_light: *LearnHub Deploy ROLLED BACK* — stayed on ${old_color} | SHA: ${IMAGE_TAG} | Env: ${DEPLOY_ENV}\"}" || true
  fi
}

# ============================================================================
# MAIN DEPLOYMENT FLOW
# ============================================================================

main() {
  log "======================================================"
  log " LearnHub Blue-Green Deployment"
  log " Environment : $DEPLOY_ENV"
  log " Image Tag   : $IMAGE_TAG"
  log " Backend     : ${BACKEND_IMAGE}:${IMAGE_TAG}"
  log " Frontend    : ${FRONTEND_IMAGE}:${IMAGE_TAG}"
  log " Dry Run     : $DRY_RUN"
  log "======================================================"

  local current_color
  current_color=$(get_current_color)
  local new_color
  new_color=$(get_new_color "$current_color")

  log "Current color: $current_color → New color: $new_color"

  # Step 1: Pull new images
  pull_images

  # Step 2: Start new containers
  start_new_containers "$new_color"

  # Step 3: Wait for health
  if ! wait_for_healthy "$new_color"; then
    rollback "$current_color" "$new_color"
    exit 1
  fi

  # Step 4: Switch Nginx
  switch_nginx_upstream "$new_color"

  # Step 5: Save new state
  set_current_color "$new_color"

  # Step 6: Drain old connections
  drain_connections

  # Step 7: Stop old containers
  stop_old_containers "$current_color"

  ok "======================================================"
  ok " Deployment complete: $current_color -> $new_color"
  ok " Image tag: $IMAGE_TAG"
  ok " Environment: $DEPLOY_ENV"
  ok "======================================================"
}

main "$@"
