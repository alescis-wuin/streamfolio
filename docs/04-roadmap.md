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

## V1.3 livrée / en validation

- Couche `MediaStorageService`.
- Mode média `classpath` par défaut.
- Mode média `local` via profil `local-media`.
- Dossiers préparés pour `originals`, `subtitles` et `hls`.
- Docker Compose avec volume média optionnel.
- Endpoints HLS protégés.
- Lecteur frontend HLS via hls.js avec fallback MP4.
- Transcodage FFmpeg local vers HLS multi-bitrate `360p`, `720p`, `1080p`.
- Master playlist HLS locale.
- Tests unitaires du stockage média.
- Tests d'intégration du streaming local.

## V1.4

- Page admin minimale : créer titre, vidéo, genres, upload de fichier.
- Modèle `MediaAsset` / `TranscodeJob`.
- Lancement de transcodage asynchrone avec statuts `PENDING`, `RUNNING`, `DONE`, `FAILED`.
- Thumbnails de timeline et preview seekbar.
- Pagination et tri enrichis côté API.
- Recherche avec pondération titre/genre/synopsis.
- Préférences utilisateur : autoplay, sous-titres, volume.
- Skeleton loaders.
- États d'erreur détaillés côté UI.
- Import/export JSON du catalogue.
- Découpage progressif de `app.js` et `styles.css`.

## V2

- PostgreSQL.
- MinIO ou S3.
- File de jobs de transcodage robuste et relançable.
- Pipeline média complet : upload, validation, transcodage, posters, thumbnails, variantes vidéo.
- HLS de production avec politique d'encodage configurable.
- Recommandations basiques.
- Profils utilisateur.
- Historique complet.

## V3

- Recherche avancée via Elasticsearch/OpenSearch.
- Observabilité : Actuator, Prometheus, Grafana.
- CDN.
- Applications packagées : Tauri/Capacitor.
- Accessibilité auditée WCAG 2.2 AA.
