# Architecture V1.1

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
        ├─ fetch JSON
        └─ flux vidéo MP4 + sous-titres
        │
        ▼
Spring Boot
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

- `auth` : connexion, jetons de démonstration, filtre bearer.
- `catalog` : catalogue, sections, détails, genres, progression, watchlist.
- `domain` : entités JPA.
- `repository` : accès aux données.
- `streaming` : flux MP4 et sous-titres.
- `seed` : données initiales idempotentes.
- `common` : erreurs API.

## Endpoints principaux

```text
GET    /api/health
POST   /api/auth/login
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
- Pas de comptes réels.
- Pas de stockage externe.
- Pas de transcodage.
- Pas de paiement, abonnement, DRM ou contenu protégé.
