#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${VALIDATION_LOG_DIR:-$ROOT_DIR/build/validation-logs}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
APP_PID=""
REDIS_STARTED_BY_VALIDATE="false"

mkdir -p "$LOG_DIR"

cleanup_app() {
  if [ -n "${APP_PID:-}" ] && kill -0 "$APP_PID" >/dev/null 2>&1; then
    kill "$APP_PID" >/dev/null 2>&1 || true
    wait "$APP_PID" >/dev/null 2>&1 || true
  fi
  APP_PID=""
}

cleanup_redis() {
  if [ "$REDIS_STARTED_BY_VALIDATE" = "true" ]; then
    (cd "$ROOT_DIR" && docker compose stop redis >/dev/null 2>&1) || true
  fi
}

cleanup() {
  cleanup_app
  cleanup_redis
}

trap cleanup EXIT

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] Missing required command: $1" >&2
    exit 1
  fi
}

run_step() {
  echo
  echo "==> $*"
  "$@"
}

start_redis() {
  if [ -n "${STREAMFOLIO_REDIS_URL:-}" ]; then
    echo "[OK] Using external Redis: $STREAMFOLIO_REDIS_URL"
    return 0
  fi

  require_command docker
  echo
  echo "==> Start Redis for session validation"
  (cd "$ROOT_DIR" && docker compose up -d redis)
  REDIS_STARTED_BY_VALIDATE="true"
  export STREAMFOLIO_REDIS_URL="redis://localhost:6379"
}

start_app() {
  local mode="$1"
  shift
  cleanup_app

  echo
  echo "==> Start application ($mode)"
  (
    cd "$ROOT_DIR"
    "$@"
  ) > "$LOG_DIR/${mode}.log" 2>&1 &
  APP_PID="$!"

  for _ in {1..45}; do
    if curl -fsS "$BASE_URL/api/health" >/dev/null 2>&1; then
      echo "[OK] Application ready ($mode)"
      return 0
    fi
    sleep 2
  done

  echo "[ERROR] Application did not start ($mode). Log:" >&2
  cat "$LOG_DIR/${mode}.log" >&2
  return 1
}

require_command bash
require_command curl
require_command git
require_command java
require_command jq
require_command mvn
require_command node
require_command python3

cd "$ROOT_DIR"

run_step bash scripts/check-clean-tree.sh
run_step bash -n scripts/*.sh
run_step python3 -m py_compile scripts/regenerate-posters.py

run_step bash -lc 'cd backend && for f in src/main/resources/static/app.js src/main/resources/static/js/*.js; do node --check --input-type=module < "$f" >/dev/null; done'
run_step bash -lc 'cd backend && node --check src/main/resources/static/csrf.js'
run_step bash -lc 'node --check playwright.config.js'
run_step bash -lc 'find tests/e2e -name "*.js" -print0 | xargs -0 -r -n1 node --check'
run_step bash -lc 'cd backend && mvn -B clean test'
run_step bash -lc 'cd backend && mvn -B -DskipTests package'

start_redis
start_app classpath java -jar "$ROOT_DIR/backend/target/streamfolio-v1-1.4.0.jar"
run_step env BASE_URL="$BASE_URL" bash "$ROOT_DIR/scripts/smoke.sh"
cleanup_app

run_step bash "$ROOT_DIR/scripts/prepare-local-media.sh" "$ROOT_DIR/backend/data/media"
start_app local-media env \
  SPRING_PROFILES_ACTIVE=local-media \
  STREAMFOLIO_MEDIA_STORAGE=local \
  STREAMFOLIO_MEDIA_ROOT="$ROOT_DIR/backend/data/media" \
  java -jar "$ROOT_DIR/backend/target/streamfolio-v1-1.4.0.jar"
run_step env BASE_URL="$BASE_URL" bash "$ROOT_DIR/scripts/smoke.sh"
cleanup_app

echo
echo "[OK] Validation completed. Logs: $LOG_DIR"
