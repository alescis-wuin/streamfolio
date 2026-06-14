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
- Nettoyage périodique des sessions expirées.
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

## V1.4 en cours

- Modèles `MediaAsset` et `TranscodeJob`.
- API admin média : jobs, assets, lancement de transcodage.
- Transcodage asynchrone observable avec statuts `PENDING`, `RUNNING`, `DONE`, `FAILED`.
- Thumbnails timeline avec manifest JSON.
- Route frontend `#/admin` pour observer et lancer les jobs.
- Découpage progressif de `app.js` en modules `api`, `utils`, `player`, `media-admin`.
- Styles admin/timeline extraits dans `media-admin.css`.
- Profil PostgreSQL.
- Profil MinIO préparé.

## V1.5

- Upload admin réel de fichier vidéo.
- Création/édition admin de titres, vidéos et genres.
- Association automatique upload → asset → job.
- Validation fichier source, taille, extension, durée et codec.
- Import/export JSON du catalogue.
- Skeleton loaders.
- États d'erreur détaillés côté UI.
- Préférences utilisateur : autoplay, sous-titres, volume.

## V2

- Adaptateur MinIO/S3 réellement branché pour les fichiers originaux et dérivés.
- File de jobs de transcodage robuste et relançable.
- Pipeline média complet : upload, validation, transcodage, posters, thumbnails, variantes vidéo.
- HLS de production avec politique d'encodage configurable.
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
