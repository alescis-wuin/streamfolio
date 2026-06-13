#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
rm -rf "$ROOT_DIR/backend/target" "$ROOT_DIR/backend/data"
echo "État local backend supprimé : backend/target et backend/data"
echo "À faire côté navigateur : vider les caches/service workers du site http://localhost:8080."
