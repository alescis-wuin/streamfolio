#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_ID="$(date +%Y%m%d-%H%M%S)"
RUN_DIR="${FULL_CHECK_LOG_DIR:-$ROOT_DIR/build/full-check/$RUN_ID}"
STEPS_DIR="$RUN_DIR/steps"
DOCKER_DIR="$RUN_DIR/docker"
VALIDATION_DIR="$RUN_DIR/validation"
ARTIFACTS_DIR="$RUN_DIR/artifacts"
BASE_URL="${BASE_URL:-http://localhost:8080}"
STEP=0
LOCAL_APP_PID=""

if [ -t 1 ]; then
  C_RESET="$(printf '\033[0m')"
  C_BLUE="$(printf '\033[34m')"
  C_GREEN="$(printf '\033[32m')"
  C_YELLOW="$(printf '\033[33m')"
  C_RED="$(printf '\033[31m')"
  C_DIM="$(printf '\033[2m')"
else
  C_RESET="" C_BLUE="" C_GREEN="" C_YELLOW="" C_RED="" C_DIM=""
fi

mkdir -p "$STEPS_DIR" "$DOCKER_DIR" "$VALIDATION_DIR" "$ARTIFACTS_DIR"

log_info() { printf '%b[INFO]%b %s\n' "$C_BLUE" "$C_RESET" "$*"; }
log_ok() { printf '%b[OK]%b   %s\n' "$C_GREEN" "$C_RESET" "$*"; }
log_warn() { printf '%b[WARN]%b %s\n' "$C_YELLOW" "$C_RESET" "$*"; }
log_err() { printf '%b[FAIL]%b %s\n' "$C_RED" "$C_RESET" "$*"; }

slug() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9._-]+/-/g; s/^-+//; s/-+$//'
}

begin_step() {
  local label="$1"
  STEP=$((STEP + 1))
  STEP_LOG_FILE="$(printf '%s/%03d_%s.log' "$STEPS_DIR" "$STEP" "$(slug "$label")")"
  printf '%b[%03d]%b %s\n' "$C_BLUE" "$STEP" "$C_RESET" "$label"
}

run_cmd() {
  local label="$1"
  shift
  begin_step "$label"
  printf '$ %q' "$@" > "$STEP_LOG_FILE"
  printf '\n\n' >> "$STEP_LOG_FILE"
  if "$@" >> "$STEP_LOG_FILE" 2>&1; then
    log_ok "$label"
  else
    log_err "$label"
    log_warn "Last 80 lines from $STEP_LOG_FILE"
    tail -n 80 "$STEP_LOG_FILE" || true
    exit 1
  fi
}

run_shell() {
  local label="$1"
  shift
  local command="$*"
  begin_step "$label"
  printf '$ %s\n\n' "$command" > "$STEP_LOG_FILE"
  if bash -lc "$command" >> "$STEP_LOG_FILE" 2>&1; then
    log_ok "$label"
  else
    log_err "$label"
    log_warn "Last 80 lines from $STEP_LOG_FILE"
    tail -n 80 "$STEP_LOG_FILE" || true
    exit 1
  fi
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    log_err "Missing command: $1"
    exit 1
  fi
}

wait_for_health() {
  local url="$1"
  local attempts="${2:-60}"
  for _ in $(seq 1 "$attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

capture_docker_logs() {
  log_info "Capturing Docker state"
  docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}' > "$DOCKER_DIR/docker-ps.txt" 2>&1 || true
  docker compose ps > "$DOCKER_DIR/docker-compose-ps.txt" 2>&1 || true
  for container in $(docker ps --format '{{.Names}}' | grep '^streamfolio-' || true); do
    docker logs "$container" > "$DOCKER_DIR/$container.log" 2>&1 || true
  done
  log_ok "Docker logs saved to $DOCKER_DIR"
}

copy_artifacts() {
  mkdir -p "$ARTIFACTS_DIR"
  if [ -d "$ROOT_DIR/backend/target/surefire-reports" ]; then
    rm -rf "$ARTIFACTS_DIR/surefire-reports"
    cp -a "$ROOT_DIR/backend/target/surefire-reports" "$ARTIFACTS_DIR/surefire-reports"
  fi
  if [ -d "$ROOT_DIR/build/playwright-report" ]; then
    rm -rf "$ARTIFACTS_DIR/playwright-report"
    cp -a "$ROOT_DIR/build/playwright-report" "$ARTIFACTS_DIR/playwright-report"
  fi
  if [ -d "$ROOT_DIR/test-results" ]; then
    rm -rf "$ARTIFACTS_DIR/test-results"
    cp -a "$ROOT_DIR/test-results" "$ARTIFACTS_DIR/test-results"
  fi
}

stop_local_app() {
  if [ -n "${LOCAL_APP_PID:-}" ] && kill -0 "$LOCAL_APP_PID" >/dev/null 2>&1; then
    kill "$LOCAL_APP_PID" >/dev/null 2>&1 || true
    wait "$LOCAL_APP_PID" >/dev/null 2>&1 || true
  fi
  LOCAL_APP_PID=""
}

trap 'stop_local_app' EXIT

start_local_maven_probe() {
  begin_step "maven spring boot run probe"
  local log_file="$STEP_LOG_FILE"
  log_info "Starting mvn spring-boot:run probe"
  (
    cd "$ROOT_DIR/backend"
    mvn spring-boot:run
  ) > "$log_file" 2>&1 &
  LOCAL_APP_PID="$!"
  if wait_for_health "$BASE_URL/api/health" 60; then
    log_ok "mvn spring-boot:run reached /api/health"
  else
    log_err "mvn spring-boot:run did not become healthy"
    tail -n 120 "$log_file" || true
    exit 1
  fi
  stop_local_app
}

run_optional_transcode_matrix() {
  if [ "${RUN_TRANSCODE_MATRIX:-false}" != "true" ]; then
    log_warn "Skipping transcode matrix. Set RUN_TRANSCODE_MATRIX=true to enable it."
    return 0
  fi
  run_shell "maven transcode matrix" "cd backend && STREAMFOLIO_RUN_TRANSCODE_MATRIX=true STREAMFOLIO_RUN_TRANSCODE_GPU=${STREAMFOLIO_RUN_TRANSCODE_GPU:-false} mvn -B -Dtest=TranscodingMediaMatrixIntegrationTest test"
  copy_artifacts
}

require_command bash
require_command curl
require_command docker
require_command git
require_command mvn
require_command node

cd "$ROOT_DIR"
log_info "Full check run: $RUN_ID"
log_info "Logs: $RUN_DIR"

run_cmd "git fetch origin" git fetch origin
run_cmd "git pull origin ff-only" git pull origin --ff-only
run_cmd "docker compose down clean" docker compose down -v --remove-orphans
run_cmd "docker compose build no-cache" docker compose build --no-cache
run_cmd "docker compose up detached" docker compose up --build -d
sleep "${DOCKER_SETTLE_SECONDS:-8}"
capture_docker_logs
run_cmd "docker ps" docker ps
run_shell "docker stop streamfolio service" "docker compose stop streamfolio && docker compose rm -f streamfolio"

run_cmd "npm e2e" npm run test:e2e
copy_artifacts
run_shell "validate" "VALIDATION_LOG_DIR=$(printf %q "$VALIDATION_DIR") bash scripts/validate.sh"
copy_artifacts
start_local_maven_probe
run_optional_transcode_matrix

log_ok "Full check completed"
printf '%bArtifacts:%b %s\n' "$C_GREEN" "$C_RESET" "$RUN_DIR"
