# Streamfolio — admin upload page replacement files

These files implement the requested split of the admin video upload into `#/admin/upload`.

Copy each file into the same path at the root of the Streamfolio repository, then run:

```bash
node --check backend/src/main/resources/static/app.js
node --check backend/src/main/resources/static/js/media-admin.js
node --check tests/e2e/admin-media.spec.js
mvn -B -pl backend test
npm test -- --project=chromium tests/e2e/admin-media.spec.js
```

Implemented changes:
- Removes the upload form from the main admin listing page.
- Adds an `Upload` button to the right of admin search/type/sort/size/filter controls.
- Adds dedicated `#/admin/upload` route and page.
- Uses one field per row and resizable textareas.
- Removes the visible duration field; duration is auto-filled from browser video metadata and falls back to backend ffprobe when possible.
- Auto-fills title, video title, year and basic defaults from the selected video filename.
- Expands accepted video extensions/MIME types, including MP4, MOV, WMV, MKV, WebM, AVI, FLV, F4V, SWF, AVCHD-style MTS/M2TS, MPEG-2-style MPEG/MPG/M2V/TS/VOB, OGV, 3GP/3G2 and MXF.
- Adds poster/backdrop previews.
- Adds timestamp-based thumbnail capture from the selected video file; controls are disabled until a video is selected.
- Adds a clickable/hoverable `?` tooltip next to Tagline.

Notes:
- Browser-side metadata/capture only works for formats/codecs the browser can decode locally.
- Backend ffprobe duration fallback requires `ffprobe` available in the runtime path or configured with `streamfolio.ffmpeg.probe-binary`.
