# Streamfolio

[![CI](https://github.com/alescis-wuin/streamfolio/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/alescis-wuin/streamfolio/actions/workflows/ci.yml)
![Version](https://img.shields.io/badge/version-1.4.0-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F)
![Redis](https://img.shields.io/badge/Redis-sessions-DC382D)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED)

Application portfolio de streaming vidéo avec Spring Boot, PWA native, PostgreSQL, Redis, Docker, HLS local, upload et administration média.

## Aperçu

![Démo Streamfolio](docs/record/demo.gif)

Captures : [`docs/screenshots/README.md`](docs/screenshots/README.md).

## Fonctionnalités

| Domaine | Couverture |
|---|---|
| Authentification | Spring Security, BCrypt, cookie `HttpOnly`/`SameSite=Strict`, logout, sessions Redis avec TTL |
| Catalogue | Films, séries, genres, recherche, filtres, rails, hero, watchlist et progression |
| Streaming | HTML5, MP4 progressif, HTTP Range, WebVTT, HLS local via hls.js |
| Admin média | Upload, métadonnées, miniatures, association série/film, jobs de transcodage |
| Persistance | H2 de démo, PostgreSQL + Flyway, Redis pour l'état de session runtime |
| Qualité | Tests Maven, tests sécurité, tests streaming/admin, smoke tests, CI GitHub Actions |

## Démarrage

```bash
git lfs install
git lfs pull
docker compose up --build
```

Développement HTTP : lancer Redis avec `docker compose up -d redis`, puis exécuter `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run` depuis `backend/`.

PostgreSQL local : lancer `docker compose up -d postgres redis`, puis exécuter `SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run` depuis `backend/`.

## Validation

```bash
bash scripts/validate.sh
npm run test:e2e
```

`validate.sh` vérifie le dépôt, les scripts, JavaScript, Python, Maven, Redis pour les sessions, les smoke tests classpath et le mode `local-media`.

## Ce que j'ai appris / Décisions techniques

- PostgreSQL conserve les données métier ; Redis conserve les sessions runtime.
- Le cookie applicatif reste `HttpOnly` et `SameSite=Strict` ; Redis ne change pas le contrat public de l'API.
- Docker Compose sert de socle d'évaluation avec PostgreSQL, Redis et MinIO.
- FFmpeg reste local pour une démonstration claire du pipeline HLS.

## Documentation

- [`docs/04-roadmap.md`](docs/04-roadmap.md)
- [`docs/08-postgresql-persistence.md`](docs/08-postgresql-persistence.md)
- [`docs/validation-checklist.md`](docs/validation-checklist.md)
- [`docs/screenshots/README.md`](docs/screenshots/README.md)

## Limites

Version portfolio : pas de DRM, pas de CDN, Redis Compose mono-nœud sans persistance disque, MinIO/S3 encore préparé mais non branché comme stockage objet complet.
