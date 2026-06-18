# Pipeline média

## Objectif

Cette phase fournit un pipeline observable pour l'ingestion, la validation, la génération et la diffusion des médias : originaux, HLS multi-bitrate, thumbnails timeline et previews administrateur.

## Modèle

```text
CatalogVideo 1 ── 0..1 MediaAsset
CatalogVideo 1 ── * TranscodeJob
```

`MediaAsset` représente l'état courant des sorties générées pour une vidéo :

- original local ou objet MinIO ;
- master playlist HLS ;
- manifest de thumbnails ;
- statut `REGISTERED`, `READY`, `MISSING` ou `FAILED`.

`TranscodeJob` représente une exécution observable :

- statut `PENDING`, `RUNNING`, `DONE`, `FAILED`, `CANCELLING`, `CANCELLED` ou `RETRYING` selon le cycle de vie ;
- progression en pourcentage ;
- dates de demande, début et fin ;
- message de diagnostic ;
- chemin de sortie ;
- possibilité de relance ou d'annulation.

## API admin

Routes protégées par l'authentification applicative et réservées au rôle `ADMIN` :

```text
GET  /api/admin/videos
GET  /api/admin/videos/ids
GET  /api/admin/videos/{videoId}
POST /api/admin/videos
PUT  /api/admin/videos/{videoId}
DELETE /api/admin/videos/{videoId}

GET  /api/admin/media/jobs
GET  /api/admin/media/jobs/{jobId}
GET  /api/admin/media/assets
POST /api/admin/media/videos/{videoId}/transcode
```

Payload de lancement :

```json
{
  "force": true
}
```

Réponse : `202 Accepted` avec le job créé.

## Publication et previews

- Les vidéos `PUBLISHED` sont accessibles par les endpoints publics authentifiés : `GET /api/videos/{id}`, `/stream`, `/hls/**`, `/subtitles`, `/thumbnails/**`.
- Les vidéos `DRAFT` sont masquées des endpoints publics avec une réponse `404`.
- Les administrateurs disposent d'endpoints explicites de preview :

```text
GET /api/admin/videos/{videoId}/preview
GET /api/admin/videos/{videoId}/preview/stream
GET /api/admin/videos/{videoId}/preview/hls/**
GET /api/admin/videos/{videoId}/preview/subtitles
GET /api/admin/videos/{videoId}/preview/thumbnails/**
```

## Sorties locales

```text
backend/data/media
├─ originals
├─ subtitles
├─ hls/{videoId}
│  ├─ master.m3u8
│  ├─ 360p/playlist.m3u8
│  ├─ 720p/playlist.m3u8
│  └─ 1080p/playlist.m3u8
└─ thumbnails/{videoId}
   ├─ manifest.json
   ├─ thumb_000.jpg
   ├─ thumb_001.jpg
   └─ ...
```

## Stockage local et MinIO

Le pipeline utilise le disque local comme staging FFmpeg. En mode `local`, les fichiers sont servis depuis `STREAMFOLIO_MEDIA_ROOT`. En mode `minio`, les originaux, sous-titres et sorties dérivées peuvent être publiés dans le bucket objet, avec fallback local pendant le traitement.

## Frontend

La route `#/admin` affiche :

- le catalogue média administrable ;
- les jobs récents ;
- les assets enregistrés ;
- un formulaire d'upload ;
- les actions de publication, liaison, suppression, transcodage, annulation et relance.

Le lecteur charge `thumbnailManifestUrl` si disponible, affiche une bande de thumbnails cliquables et propose un sélecteur manuel de qualité HLS lorsque la lecture utilise hls.js. Le mode `Auto` reste le comportement par défaut.

## Profils PostgreSQL et MinIO

Profil PostgreSQL :

```bash
SPRING_PROFILES_ACTIVE=postgres
```

Profil production recommandé : PostgreSQL, Redis durable, stockage MinIO, validation source stricte et cookies de session sécurisés. Le démarrage en profil `prod` échoue si les cookies de session ne sont pas configurés en mode sécurisé.
