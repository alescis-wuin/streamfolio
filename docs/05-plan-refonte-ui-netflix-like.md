# Plan de refonte UI/UX réalisé

## Objectif

Transformer la V1 en une démonstration plus crédible de plateforme de streaming moderne, avec une direction proche des standards Netflix/streaming : immersion, rails de contenus, navigation directe, catalogue filtrable et lecteur sobre.

## Étapes frontend réalisées

1. Refonte de la structure shell : topbar, nav principale, recherche globale, profil, logout.
2. Ajout d'une navigation mobile fixe.
3. Création d'un hero immersif plein écran avec image de fond, vignettage et actions principales.
4. Refonte des sections en rails horizontaux avec boutons de scroll.
5. Refonte des cartes : format 16:9, lecture au survol, progression, description tronquée, orbe détail.
6. Ajout d'une zone de découverte par genres.
7. Refonte du catalogue avec filtres type + genre.
8. Ajout de la route `#/my-list`.
9. Refonte des fiches détail : backdrop, poster, métadonnées, genres, épisodes.
10. Amélioration du lecteur : raccourcis clavier, sauvegarde forcée sur pause/quitte, retour fiche.
11. Mise à jour PWA : cache service worker et manifest.
12. Amélioration responsive écran large/mobile.
13. Respect de `prefers-reduced-motion`.

## Étapes backend réalisées

1. Ajout de `GET /api/genres`.
2. Ajout de `CatalogService.genres()`.
3. Seed catalog idempotent par slug.
4. Catalogue enrichi de 4 à 10 titres.
5. Ajout d'affiches SVG abstraites originales.
6. Réutilisation des vidéos locales existantes pour garder le projet léger et légalement neutre.

## Étapes recommandées ensuite

1. Ajouter des tests unitaires sur `CatalogService`.
2. Ajouter tests MVC sur `/api/catalog`, `/api/genres`, `/api/sections`.
3. Ajouter snapshots visuels ou tests Playwright si migration Node.
4. Ajouter génération de thumbnails via FFmpeg.
5. Ajouter vrais profils utilisateurs.
6. Ajouter Spring Security et JWT signé.
7. Ajouter HLS pour streaming adaptatif.
