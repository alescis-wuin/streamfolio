#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
COOKIE_JAR="$(mktemp)"
trap 'rm -f "$COOKIE_JAR"' EXIT

curl -fsS "$BASE_URL/api/health" | jq .
for i in {1..30}; do
  if curl -fsS -c "$COOKIE_JAR" -X POST "$BASE_URL/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"email":"alexis@example.dev","password":"demo1234"}' | jq '.user.email'; then
    break
  fi
  if [ "$i" -eq 30 ]; then
    exit 1
  fi
  sleep 1
done

curl -fsS -b "$COOKIE_JAR" "$BASE_URL/api/me" | jq .
curl -fsS -b "$COOKIE_JAR" "$BASE_URL/api/sections" | jq '.sections | length'
curl -fsS -b "$COOKIE_JAR" "$BASE_URL/api/genres" | jq .
curl -fsS -b "$COOKIE_JAR" "$BASE_URL/api/catalog?type=MOVIE" | jq 'length'
curl -fsSI -b "$COOKIE_JAR" -H 'Range: bytes=0-1023' "$BASE_URL/api/videos/1/stream" | grep -Ei 'HTTP/|accept-ranges|content-type|content-length|content-range' || true
curl -fsS -b "$COOKIE_JAR" -X POST "$BASE_URL/api/auth/logout" -o /dev/null
