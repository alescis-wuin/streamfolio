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

## V1.2 / Phase 2.5 en cours de stabilisation

- Spring Security intégré.
- Mot de passe de démonstration encodé en BCrypt.
- Session courte via cookie `HttpOnly` et `SameSite=Strict`.
- Logout serveur.
- Endpoints vidéo protégés.
- Erreurs serveur génériques côté client et loggées côté serveur.
- Requêtes catalogue optimisées avec préchargement du contexte utilisateur.
- Tests unitaires `AuthService`.
- Tests sécurité MockMvc.
- Tests d'intégration `CatalogService`.
- Smoke test HTTP basé sur cookie de session.
- Checklist de validation phase 2.5.

## V1.3

- Pagination et tri côté API.
- Recherche avec pondération titre/genre/synopsis.
- Préférences utilisateur : autoplay, sous-titres, volume.
- Skeleton loaders.
- États d'erreur détaillés côté UI.
- Import/export JSON du catalogue.
- Découpage progressif de `app.js` et `styles.css`.

## V2

- PostgreSQL.
- MinIO ou S3.
- FFmpeg pipeline : transcodage, posters, thumbnails.
- HLS/DASH adaptatif.
- Recommandations basiques.
- Profils utilisateur.
- Historique complet.

## V3

- Recherche avancée via Elasticsearch/OpenSearch.
- Observabilité : Actuator, Prometheus, Grafana.
- CDN.
- Applications packagées : Tauri/Capacitor.
- Accessibilité auditée WCAG 2.2 AA.
