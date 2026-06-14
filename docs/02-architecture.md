# Architecture V1.3

## Vue logique

```text
Navigateur / PWA
  ├─ accueil streaming premium
  ├─ rails horizontaux
  ├─ catalogue filtré et paginé
  ├─ ma liste
  ├─ fiches détail
  └─ lecteur vidéo HTML5 + hls.js
        │
        ├─ fetch JSON avec cookie de session same-origin
        ├─ flux MP4 progressif protégé
        ├─ flux HLS local protégé
        └─ sous-titres WebVTT protégés
        │
        ▼
Spring Boot
  ├─ SecurityConfig
  ├─ AuthFilter
  ├─ AuthController
  ├─ CatalogController
  ├─ StreamingController
  ├─ CatalogService
  ├─ AuthService
  ├─ MediaStorageService
  ├─ TranscodingService
  ├─ FfmpegService
  └─ JPA repositories
        │
        ▼
H2 en mémoire + médias classpath ou backend/data/media
```

## Modules backend

- `config` : configuration Spring Security.
- `auth` : login, logout, BCrypt, cookie de session, filtre d'authentification et nettoyage des sessions expirées.
- `catalog` : catalogue, sections, détails, genres, pagination, progression, watchlist.
- `domain` : entités JPA.
- `repository` : accès aux données avec fetch plan paginé pour limiter les N+1.
- `streaming` : flux MP4, HLS local et sous-titres.
- `transcoding` : statut FFmpeg et génération HLS locale multi-bitrate.
- `seed` : données initiales idempotentes.
- `common` : erreurs API.

## Sécurité applicative

- Authentification via Spring Security.
- Mot de passe de démonstration encodé en BCrypt.
- Session applicative stockée côté serveur, avec durée de vie configurable.
- Cookie `HttpOnly`, `SameSite=Strict`, `Path=/`.
- Option `Secure` configurable par variable d'environnement pour un déploiement HTTPS.
- Endpoints vidéo protégés : metadata, stream MP4, playlists HLS, segments HLS, sous-titres et progression.
- CSRF actif sur les requêtes mutantes same-origin.
- Logout serveur avec invalidation de session.

## Endpoints principaux

```text
GET    /api/health
GET    /api/csrf
POST   /api/auth/login
POST   /api/auth/logout
GET    /api/me
GET    /api/sections
GET    /api/genres
GET    /api/catalog?query=&type=&genre=&page=&size=
GET    /api/catalog/{slug}
GET    /api/videos/{videoId}
GET    /api/videos/{videoId}/stream
GET    /api/videos/{videoId}/hls/**
GET    /api/videos/{videoId}/subtitles
PUT    /api/videos/{videoId}/progress
POST   /api/titles/{titleId}/watchlist
DELETE /api/titles/{titleId}/watchlist
```

## Modèle de données

```text
UserAccount 1 ── * UserProgress * ── 1 CatalogVideo * ── 1 CatalogTitle
UserAccount 1 ── * WatchlistItem * ── 1 CatalogTitle
CatalogTitle 1 ── * CatalogVideo
CatalogTitle 1 ── * genres
```

## Stockage et streaming

```text
classpath
  └─ backend/src/main/resources/media

local-media
  └─ backend/data/media
       ├─ originals
       ├─ subtitles
       └─ hls/{videoId}
            ├─ master.m3u8
            ├─ 360p/playlist.m3u8
            ├─ 720p/playlist.m3u8
            └─ 1080p/playlist.m3u8
```

Le lecteur reçoit toujours `streamUrl` pour le fallback MP4. En mode `local-media`, si `master.m3u8` existe, le DTO playback ajoute `hlsUrl` et `streamingMode=HLS_AVAILABLE`.

## Limites assumées

- Base H2 en mémoire par défaut, avec profil optionnel `persistent`.
- Compte de démonstration préchargé.
- Sessions en mémoire, donc non partagées entre plusieurs instances.
- Stockage média local démonstratif ; pas encore de stockage objet S3/MinIO.
- Transcodage FFmpeg local sans file de jobs ni page d'administration.
- Pas de paiement, abonnement, DRM, CDN ou contenu protégé réel.
