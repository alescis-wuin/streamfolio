#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"

curl -fsS "$BASE_URL/api/health" | jq .
TOKEN=$(curl -fsS -X POST "$BASE_URL/api/auth/login"   -H 'Content-Type: application/json'   -d '{"email":"alexis@example.dev","password":"demo1234"}' | jq -r .token)

curl -fsS "$BASE_URL/api/me" -H "Authorization: Bearer $TOKEN" | jq .
curl -fsS "$BASE_URL/api/sections" -H "Authorization: Bearer $TOKEN" | jq '.sections | length'
curl -fsS "$BASE_URL/api/genres" -H "Authorization: Bearer $TOKEN" | jq .
curl -fsS "$BASE_URL/api/catalog?type=MOVIE" -H "Authorization: Bearer $TOKEN" | jq 'length'
curl -fsSI -H 'Range: bytes=0-1023' "$BASE_URL/api/videos/1/stream" | grep -Ei 'HTTP/|accept-ranges|content-type|content-length|content-range' || true
