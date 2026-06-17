#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
COOKIE_JAR="$(mktemp)"
trap 'rm -f "$COOKIE_JAR"' EXIT

curl -fsS "$BASE_URL/api/health" | jq .

CSRF_PAYLOAD="$(curl -fsS -c "$COOKIE_JAR" -b "$COOKIE_JAR" "$BASE_URL/api/csrf")"
CSRF_HEADER="$(printf '%s' "$CSRF_PAYLOAD" | jq -r '.headerName')"
CSRF_TOKEN="$(printf '%s' "$CSRF_PAYLOAD" | jq -r '.token')"

LOGIN_OK="false"
for i in {1..30}; do
  if curl -fsS -c "$COOKIE_JAR" -b "$COOKIE_JAR" -X POST "$BASE_URL/api/auth/login" \
    -H 'Content-Type: application/json' \
    -H "$CSRF_HEADER: $CSRF_TOKEN" \
    -d '{"email":"alexis@example.dev","password":"demo1234"}' 2>/dev/null | jq '.user.email'; then
    LOGIN_OK="true"
    break
  fi
  sleep 1
done

if [ "$LOGIN_OK" != "true" ]; then
  echo "[ERROR] Smoke login failed after 30 attempts." >&2
  exit 1
fi

curl -fsS -b "$COOKIE_JAR" "$BASE_URL/api/me" | jq .
curl -fsS -b "$COOKIE_JAR" "$BASE_URL/api/sections" | jq '.sections | length'
curl -fsS -b "$COOKIE_JAR" "$BASE_URL/api/genres" | jq .
curl -fsS -b "$COOKIE_JAR" "$BASE_URL/api/catalog?type=MOVIE&page=0&size=12" | jq '.items | length'
curl -fsS -b "$COOKIE_JAR" "$BASE_URL/api/catalog?query=botanical&type=SERIES&genre=Botanique&page=0&size=2" | jq '.pagination.totalElements'
curl -fsSI -b "$COOKIE_JAR" -H 'Range: bytes=0-1023' "$BASE_URL/api/videos/1/stream" | grep -Ei 'HTTP/|accept-ranges|content-type|content-length|content-range' || true
curl -fsS -b "$COOKIE_JAR" -X POST "$BASE_URL/api/auth/logout" -H "$CSRF_HEADER: $CSRF_TOKEN" -o /dev/null
