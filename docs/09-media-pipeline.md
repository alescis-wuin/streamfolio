# Pipeline média

## Objectif

Cette phase ajoute un socle observable pour la génération locale de médias dérivés : HLS multi-bitrate et thumbnails timeline.

## Modèle

```text
CatalogVideo 1 ── 0..1 MediaAsset
CatalogVideo 1 ── * TranscodeJob
```

`MediaAsset` représente l'état courant des sorties générées pour une vidéo :

- original local ;
- master playlist HLS ;
- manifest de thumbnails ;
- statut `REGISTERED`, `READY`, `MISSING` ou `FAILED`.

`TranscodeJob` représente une exécution observable :

- statut `PENDING`, `RUNNING`, `DONE` ou `FAILED` ;
- progression en pourcentage ;
- dates de demande, début et fin ;
- message de diagnostic ;
- chemin de sortie.

## API admin

Routes protégées par l'authentification applicative :

```text
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

## Frontend

Une route `#/admin` affiche :

- les jobs récents ;
- les assets enregistrés ;
- un formulaire minimal pour lancer le transcodage d'une vidéo par ID.

Le lecteur charge `thumbnailManifestUrl` si disponible et affiche une bande de thumbnails cliquables.

## Profils PostgreSQL et MinIO

Profil PostgreSQL :

```bash
SPRING_PROFILES_ACTIVE=postgres
```

Profil PostgreSQL + MinIO via Docker Compose :

```bash
SPRING_PROFILES_ACTIVE=postgres,minio docker compose up --build
```

MinIO est préparé comme profil de configuration. L'adaptateur objet complet reste une étape ultérieure : le pipeline continue d'écrire localement dans `STREAMFOLIO_MEDIA_ROOT`.
