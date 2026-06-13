#!/usr/bin/env bash
set -euo pipefail

VIDEO_ID="${1:-1}"
FILENAME="${2:-aurora-drift.mp4}"
MEDIA_ROOT="${STREAMFOLIO_MEDIA_ROOT:-backend/data/media}"
FFMPEG="${STREAMFOLIO_FFMPEG_BINARY:-ffmpeg}"
SEGMENT_TIME="${STREAMFOLIO_HLS_SEGMENT_TIME:-4}"

SOURCE="$MEDIA_ROOT/originals/$FILENAME"
OUTPUT_DIR="$MEDIA_ROOT/hls/$VIDEO_ID"
PLAYLIST="$OUTPUT_DIR/master.m3u8"

if [ ! -f "$SOURCE" ]; then
  bash scripts/prepare-local-media.sh "$MEDIA_ROOT"
fi

if [ ! -f "$SOURCE" ]; then
  echo "Source video not found: $SOURCE" >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

"$FFMPEG" -y \
  -i "$SOURCE" \
  -map 0:v:0 \
  -map '0:a:0?' \
  -c:v libx264 \
  -preset veryfast \
  -crf 23 \
  -pix_fmt yuv420p \
  -flags +cgop \
  -g "$((SEGMENT_TIME * 24))" \
  -sc_threshold 0 \
  -c:a aac \
  -b:a 128k \
  -ac 2 \
  -f hls \
  -hls_time "$SEGMENT_TIME" \
  -hls_list_size 0 \
  -hls_playlist_type vod \
  -hls_segment_filename "$OUTPUT_DIR/segment_%03d.ts" \
  "$PLAYLIST"

printf 'HLS playlist generated: %s\n' "$PLAYLIST"
