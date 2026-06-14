#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-backend/data/media}"
SOURCE="backend/src/main/resources/media"

if [ ! -d "$SOURCE" ]; then
  echo "Source media directory not found: $SOURCE" >&2
  exit 1
fi

mkdir -p "$ROOT/originals" "$ROOT/subtitles" "$ROOT/hls" "$ROOT/thumbnails"

for file in "$SOURCE"/*.mp4; do
  [ -e "$file" ] || continue
  cp -n "$file" "$ROOT/originals/"
done

for file in "$SOURCE"/*.vtt; do
  [ -e "$file" ] || continue
  cp -n "$file" "$ROOT/subtitles/"
done

printf 'Local media storage prepared in %s\n' "$ROOT"
printf 'Originals: %s/originals\n' "$ROOT"
printf 'Subtitles: %s/subtitles\n' "$ROOT"
printf 'HLS placeholder: %s/hls\n' "$ROOT"
printf 'Thumbnails placeholder: %s/thumbnails\n' "$ROOT"
