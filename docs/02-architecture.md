# Architecture V1.2

## Vue logique

```text
Navigateur / PWA
  ├─ accueil streaming premium
  ├─ rails horizontaux
  ├─ catalogue filtré
  ├─ ma liste
  ├─ fiches détail
  └─ lecteur vidéo HTML5
        │
        ├─ fetch JSON avec cookie de session same-origin
        └─ flux vidéo MP4 + sous-titres protégés
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
  └─ JPA repositories
        │
        ▼
H2 en mémoire + médias classpath
```

## Modules backend

- `config` : configuration Spring Security.
- `auth` : login, logout, BCrypt, cookie de session, filtre d'authentification.
- `catalog` : catalogue, sections, détails, genres, progression, watchlist.
- `domain` : entités JPA.
- `repository` : accès aux données, avec préchargement ciblé pour limiter les N+1.
- `streaming` : flux MP4 et sous-titres.
- `seed` : données initiales idempotentes.
- `common` : erreurs API.

## Sécurité applicative

- Authentification via Spring Security.
- Mot de passe de démonstration encodé en BCrypt.
- Session applicative stockée côté serveur, avec durée de vie configurable.
- Cookie `HttpOnly`, `SameSite=Strict`, `Path=/`.
- Option `Secure` configurable par variable d'environnement pour un déploiement HTTPS.
- Endpoints vidéo protégés : metadata, stream, sous-titres et progression.
- Logout serveur avec invalidation de session.

## Endpoints principaux

```text
GET    /api/health
POST   /api/auth/login
POST   /api/auth/logout
GET    /api/me
GET    /api/sections
GET    /api/genres
GET    /api/catalog?query=&type=&genre=
GET    /api/catalog/{slug}
GET    /api/videos/{videoId}
GET    /api/videos/{videoId}/stream
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

## Limites assumées

- Base H2 en mémoire par défaut, avec profil optionnel `persistent`.
- Compte de démonstration préchargé.
- Sessions en mémoire, donc non partagées entre plusieurs instances.
- Pas de stockage externe.
- Pas de transcodage.
- Pas de paiement, abonnement, DRM ou contenu protégé réel.
