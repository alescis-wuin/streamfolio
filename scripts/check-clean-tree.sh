#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v git >/dev/null 2>&1; then
  echo "[ERROR] git is required to inspect tracked files." >&2
  exit 1
fi

matches="$(git ls-files | grep -E '(^|/)target/|\.class$|(^|/)__pycache__/|\.pyc$' || true)"

if [ -n "$matches" ]; then
  echo "[ERROR] Generated files are tracked by Git:" >&2
  echo "$matches" >&2
  echo >&2
  echo "Remove them with:" >&2
  echo "  git rm -r --cached backend/target '**/*.class' '**/__pycache__' '**/*.pyc'" >&2
  exit 1
fi

echo "[OK] No generated files tracked: backend/target/, *.class, __pycache__, *.pyc"
