# Roadmap

## V1.1 livrée

- Backend Spring Boot.
- Catalogue films/séries enrichi.
- Streaming MP4 progressif.
- Sous-titres.
- Progression utilisateur.
- Watchlist.
- Endpoint genres.
- PWA responsive.
- Refonte UI streaming premium.

## V1.2 livrée

- Spring Security intégré.
- Mot de passe de démonstration encodé en BCrypt.
- Session courte via cookie `HttpOnly` et `SameSite=Strict`.
- Logout serveur.
- Endpoints vidéo protégés.
- CSRF sur les mutations.
- Erreurs serveur génériques côté client et loggées côté serveur.
- Requêtes catalogue paginées avec fetch plan dédié.
- Test de non-régression sur le nombre de requêtes SQL du catalogue.
- Tests unitaires `AuthService`.
- Tests sécurité MockMvc.
- Tests d'intégration `CatalogService`.
- Smoke test HTTP basé sur cookie de session.
- Checklist de validation.

## V1.3 livrée

- Couche `MediaStorageService`.
- Mode média `classpath` par défaut.
- Mode média `local` via profil `local-media`.
- Dossiers préparés pour `originals`, `subtitles`, `hls` et `thumbnails`.
- Docker Compose avec volume média optionnel.
- Endpoints HLS protégés.
- Lecteur frontend HLS via hls.js avec fallback MP4.
- Transcodage FFmpeg local vers HLS multi-bitrate `360p`, `720p`, `1080p`.
- Master playlist HLS locale.
- Tests unitaires du stockage média.
- Tests d'intégration du streaming local.

## V1.4 livrée

- Modèles `MediaAsset` et `TranscodeJob`.
- API admin média : jobs, assets, lancement de transcodage.
- Transcodage asynchrone observable avec statuts `PENDING`, `RUNNING`, `DONE`, `FAILED`.
- Thumbnails timeline avec manifest JSON.
- Route frontend `#/admin` pour observer et lancer les jobs.
- Découpage progressif de `app.js` en modules `api`, `utils`, `player`, `media-admin`.
- Styles admin/timeline extraits dans `media-admin.css`.
- Profil PostgreSQL.
- Profil MinIO préparé.
- Sessions runtime externalisées dans Redis.
- Version Maven et README alignés sur `1.4.0`.

## V1.5 livrée

- Sélecteur manuel de qualité HLS côté lecteur hls.js, avec mode automatique par défaut.
- Validation source renforcée par signature, durée, présence de flux vidéo et politique codec.
- États de publication explicites `DRAFT` / `PUBLISHED`.
- Médias non publiés masqués des endpoints publics `playback`, `stream`, HLS, sous-titres et thumbnails.
- Preview des médias non publiés via endpoints admin explicites.
- `/api/admin/**` réservé au rôle `ADMIN`.
- Profil `prod` refusé si les cookies de session ne sont pas sécurisés.
- Pagination base de données côté catalogue admin.

## V1.6

- Import/export JSON du catalogue.
- Skeleton loaders.
- États d'erreur détaillés côté UI.
- Préférences utilisateur : autoplay, sous-titres, volume.

## V2

- File de jobs de transcodage robuste et relançable sur environnement distribué.
- HLS de production avec politique d'encodage configurable par environnement.
- PostgreSQL comme profil recommandé hors démo locale.
- Recommandations basiques.
- Profils utilisateur.
- Historique complet.

## V3

- Recherche avancée via Elasticsearch/OpenSearch.
- Observabilité : Actuator, Prometheus et Grafana.
- CDN.
- Applications packagées : Tauri/Capacitor.
- Accessibilité auditée WCAG 2.2 AA.
