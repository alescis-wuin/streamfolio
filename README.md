# Streamfolio

[![CI](https://github.com/alescis-wuin/streamfolio/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/alescis-wuin/streamfolio/actions/workflows/ci.yml)
![Version](https://img.shields.io/badge/version-1.4.0-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F)
![Redis](https://img.shields.io/badge/Redis-sessions-DC382D)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED)

Application portfolio de streaming vidéo avec Spring Boot, PWA native, PostgreSQL, Redis, Docker, HLS local, MinIO, upload et administration média.

## Aperçu

![Démo Streamfolio](docs/record/demo.gif)

Captures : [`docs/screenshots/README.md`](docs/screenshots/README.md).

## Fonctionnalités

| Domaine | Couverture |
|---|---|
| Authentification | Spring Security, BCrypt, cookie `HttpOnly`/`SameSite=Strict`, logout, sessions Redis avec TTL |
| Catalogue | Films, séries, genres, recherche, filtres, rails, hero, watchlist, progression et états `DRAFT` / `PUBLISHED` côté vidéo |
| Streaming | HTML5, MP4 progressif, HTTP Range, WebVTT, HLS local/MinIO via hls.js |
| Admin média | Upload mémoire-first pour petits fichiers, métadonnées, miniatures, publication `DRAFT/PUBLISHED`, annulation et relance manuelle des jobs |
| Pipeline média | File persistante, workers asynchrones bornés, ordonnanceur, reprise après redémarrage, retry exponentiel, annulation FFmpeg coopérative |
| Persistance | H2 de démo, PostgreSQL + Flyway, Redis pour l'état de session runtime, MinIO pour originaux, sous-titres et sorties HLS/thumbnails générées |
| Qualité | Tests Maven, tests sécurité, tests streaming/admin, smoke tests, E2E Playwright, CI GitHub Actions |

## Démarrage

```bash
git lfs install
git lfs pull
docker compose up --build
```

Développement admin/HLS via Maven : utiliser `bash scripts/run.sh`. Ce script prépare `backend/data/media`, démarre Redis et lance le profil `local-media`.

Commande équivalente :

```bash
docker compose up -d redis
bash scripts/prepare-local-media.sh backend/data/media
cd backend
SPRING_PROFILES_ACTIVE=local-media mvn spring-boot:run
```

Profil local explicite :

```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

PostgreSQL local : lancer `docker compose up -d postgres redis`, puis exécuter `SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run` depuis `backend/`.

## Production

Profil recommandé :

```bash
SPRING_PROFILES_ACTIVE=prod
STREAMFOLIO_COOKIE_SECURE=true
STREAMFOLIO_REDIS_URL=redis://redis:6379
STREAMFOLIO_POSTGRES_URL=jdbc:postgresql://postgres:5432/streamfolio
STREAMFOLIO_POSTGRES_USER=streamfolio
STREAMFOLIO_POSTGRES_PASSWORD=change-me
STREAMFOLIO_MINIO_ENABLED=true
STREAMFOLIO_MEDIA_STORAGE=minio
STREAMFOLIO_MINIO_ENDPOINT=http://minio:9000
STREAMFOLIO_MINIO_PUBLIC_ENDPOINT=https://media.example.dev
STREAMFOLIO_MINIO_BUCKET=streamfolio-media
STREAMFOLIO_MINIO_ACCESS_KEY=streamfolio
STREAMFOLIO_MINIO_SECRET_KEY=change-me
STREAMFOLIO_SOURCE_VALIDATION_REJECT_INVALID=true
STREAMFOLIO_SOURCE_VALIDATION_REQUIRE_DURATION=true
STREAMFOLIO_SOURCE_VALIDATION_REQUIRE_VIDEO_STREAM=true
STREAMFOLIO_HLS_VARIANTS=360p:640:360:800k:1000000,720p:1280:720:2800k:3200000,1080p:1920:1080:5000k:5600000
```

En production, placer Streamfolio derrière HTTPS et conserver `STREAMFOLIO_COOKIE_SECURE=true`. Redis doit être durable ou externalisé si les sessions doivent survivre à une coupure de service Redis. PostgreSQL conserve les données métier ; Redis conserve les sessions runtime ; MinIO conserve les médias originaux, sous-titres et sorties HLS/thumbnails générées, avec disque local comme staging FFmpeg.

## Validation

```bash
bash scripts/validate.sh
npm run test:e2e
```

`validate.sh` vérifie le dépôt, les scripts, JavaScript, Python, Maven, Redis pour les sessions, les smoke tests classpath et le mode `local-media`.

Un workflow dédié `Main Merge Validation` relance la validation complète après merge ou push sur `main`.

## Ce que j'ai appris / Décisions techniques

- PostgreSQL conserve les données métier ; Redis conserve les sessions runtime.
- Le cookie applicatif reste `HttpOnly` et `SameSite=Strict` ; Redis ne change pas le contrat public de l'API.
- Docker Compose sert de socle d'évaluation avec PostgreSQL, Redis et MinIO.
- FFmpeg reste local pour le transcodage ; MinIO sert de stockage objet pour les originaux et les sorties générées.
- Les jobs de transcodage sont persistés, relançables par l'ordonnanceur et annulables pendant les processus FFmpeg.
- La validation avancée combine extension, MIME, signature de conteneur, durée, présence de flux vidéo et politique codec.

## Documentation

- [`docs/04-roadmap.md`](docs/04-roadmap.md)
- [`docs/08-postgresql-persistence.md`](docs/08-postgresql-persistence.md)
- [`docs/validation-checklist.md`](docs/validation-checklist.md)
- [`docs/screenshots/README.md`](docs/screenshots/README.md)

## Limites

Version portfolio : pas de DRM, pas de CDN, Redis Compose mono-nœud. Le pipeline MinIO reste un stockage objet local de démonstration, pas une architecture multi-région ni CDN.
