#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROFILE="${SPRING_PROFILES_ACTIVE:-local-media}"
MEDIA_ROOT="${STREAMFOLIO_MEDIA_ROOT:-$ROOT_DIR/backend/data/media}"

cd "$ROOT_DIR"
docker compose up -d redis
bash scripts/prepare-local-media.sh "$MEDIA_ROOT"

cd "$ROOT_DIR/backend"
SPRING_PROFILES_ACTIVE="$PROFILE" \
STREAMFOLIO_REDIS_URL="${STREAMFOLIO_REDIS_URL:-redis://localhost:6379}" \
STREAMFOLIO_MEDIA_ROOT="$MEDIA_ROOT" \
mvn spring-boot:run
