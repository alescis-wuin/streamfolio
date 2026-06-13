# Recherche synthétique

## Patterns streaming retenus

Les interfaces de plateformes de streaming convergent autour de quelques patterns : une mise en avant éditoriale, des rails horizontaux, une reprise de lecture, une liste personnelle, des pages détail riches, une recherche visible et un lecteur très concentré sur la vidéo.

Les annonces publiques de Netflix en 2025 mettent en avant une navigation supérieure plus simple, des recommandations plus réactives et une section centralisée de type “My Netflix”. La refonte Streamfolio reprend ces principes génériques sans copier les logos, textes, contenus protégés ou identité de Netflix.

Décisions retenues :

- hero immersif en haut de page ;
- navigation supérieure : Accueil, Séries, Films, Ma liste, Catalogue ;
- rails horizontaux pour la découverte ;
- cartes 16:9 adaptées aux écrans larges ;
- fiche détail séparée pour éviter de surcharger l'accueil ;
- recherche globale visible ;
- filtres par type et genre ;
- pas d'autoplay intrusif ;
- action “Lecture” immédiate et action “Plus d'infos” distincte ;
- page “Ma liste” dédiée ;
- lecteur vidéo sobre, sans éléments décoratifs autour de la vidéo.

## Backend Java / Spring

Spring Boot reste adapté à une V1 portfolio : application autonome, serveur embarqué, configuration réduite, endpoints REST et intégration simple avec une PWA servie en statique.

Spring MVC sait gérer les requêtes HTTP Range quand un contrôleur retourne une `Resource` ou une `ResponseEntity<Resource>`. Cela rend le MP4 progressif suffisant pour une démonstration locale : démarrage simple, support du seek vidéo par le navigateur, pas de pipeline FFmpeg obligatoire.

## Streaming

La V1.1 utilise du MP4 progressif avec requêtes Range. C'est simple, testable et suffisant pour un portfolio.

Évolutions production recommandées :

- transcodage HLS/DASH ;
- stockage objet S3/MinIO ;
- CDN ;
- débit adaptatif ;
- thumbnails générés ;
- prévisualisation contrôlée ;
- DRM uniquement si nécessaire et légalement justifié.

## Accessibilité

La V1.1 inclut :

- lecteur vidéo natif ;
- sous-titres WebVTT ;
- navigation clavier ;
- focus visible ;
- contrastes élevés ;
- réduction des animations si `prefers-reduced-motion` est actif ;
- structure HTML sémantique ;
- boutons avec libellés explicites ;
- absence d'autoplay par défaut.

## Sources consultées

- Netflix Tudum — New TV layout, 2025 : https://www.netflix.com/tudum/articles/netflix-new-tv-layout
- Netflix About — New TV experience, 2025 : https://about.netflix.com/news/unveiling-our-innovative-new-tv-experience
- Netflix Help — Keyboard shortcuts : https://help.netflix.com/en/node/24855
- Netflix Tech Blog — Artwork personalization : https://netflixtechblog.com/artwork-personalization-at-netflix-c589f074ad76
- Spring Boot documentation : https://docs.spring.io/spring-boot/documentation.html
- Spring MVC Range Requests : https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-range.html
- MDN HTTP Range Requests : https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Range_requests
- W3C WCAG 2.2 : https://www.w3.org/TR/WCAG22/
- W3C Focus Appearance : https://www.w3.org/WAI/WCAG22/Understanding/focus-appearance.html
