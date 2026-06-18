# Media pipeline validation

## Full local diagnostic run

Run from the repository root:

```bash
bash scripts/full-check.sh
```

The script creates a unique directory under `build/full-check/<timestamp>/` and stores:

- one log file per command in `steps/`;
- Docker state and one log file per Streamfolio container in `docker/`;
- validation logs in `validation/`;
- copied Maven Surefire reports in `artifacts/surefire-reports/`;
- copied Playwright reports and test results when available.

The console output is intentionally concise: each phase is numbered, colored, and only prints the tail of a failed log when a command fails.

## Commands covered by the full check

The script performs the same workflow usually executed manually:

1. `git fetch origin`;
2. `git pull origin --ff-only`;
3. `docker compose down -v --remove-orphans`;
4. `docker compose build --no-cache`;
5. `docker compose up --build -d`;
6. `docker ps` and per-container `docker logs` capture;
7. stop/remove the Streamfolio app service while keeping infrastructure containers available;
8. `npm run test:e2e`;
9. `bash scripts/validate.sh` with a dedicated `VALIDATION_LOG_DIR`;
10. a bounded `mvn spring-boot:run` probe that waits for `/api/health` and stops the app.

## Optional real transcoding matrix

The heavy FFmpeg matrix is opt-in because it generates and transcodes many containers.

CPU/local and CPU/MinIO-backed coverage:

```bash
RUN_TRANSCODE_MATRIX=true bash scripts/full-check.sh
```

Direct Maven invocation:

```bash
cd backend
STREAMFOLIO_RUN_TRANSCODE_MATRIX=true mvn -B -Dtest=TranscodingMediaMatrixIntegrationTest test
```

Optional GPU-profile lots:

```bash
cd backend
STREAMFOLIO_RUN_TRANSCODE_MATRIX=true \
STREAMFOLIO_RUN_TRANSCODE_GPU=true \
STREAMFOLIO_TEST_GPU_ENCODER=h264_nvenc \
mvn -B -Dtest=TranscodingMediaMatrixIntegrationTest test
```

The GPU-profile tests are skipped unless `STREAMFOLIO_RUN_TRANSCODE_GPU=true` is set. They also skip when the requested encoder is not advertised by the local FFmpeg build.

## Matrix assertions

The matrix verifies:

- accepted container fixtures generated through FFmpeg;
- local CPU HLS transcode over the accepted format list;
- MinIO-backed CPU publication through the media gateway;
- optional local GPU-profile transcode;
- optional MinIO-backed GPU-profile publication;
- `master.m3u8` existence and variant references;
- variant playlists and generated `.ts` segments;
- segment dimensions through `ffprobe`;
- thumbnail manifest and expected thumbnail count.
