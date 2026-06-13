# Changelog

Historique détaillé des versions Streamfolio.

## V1.1 — refonte UI streaming premium

Plateforme de streaming vidéo de démonstration pour portfolio : backend Java/Spring Boot, interface PWA responsive, catalogue films/séries, lecteur vidéo, progression, recherche, filtres par genre et liste personnelle.

Cette version conserve une architecture simple et testable : le frontend PWA est servi directement par Spring Boot, sans build Node obligatoire. La refonte vise une expérience proche des plateformes de streaming modernes : hero immersif, rails horizontaux, cartes 16:9, navigation supérieure, recherche globale, page catalogue filtrable, page “Ma liste”, lecteur accessible et bruit visuel réduit.

### Fonctionnalités V1.1

- Authentification de démonstration avec jeton en mémoire.
- Catalogue films/séries enrichi à 18 titres de démonstration.
- Sections : continuer, ma liste, nouveautés, films, séries.
- Hero immersif avec image de fond, CTA lecture, détails et watchlist.
- Rails horizontaux inspirés des plateformes de streaming.
- Cartes 16:9 avec lecture au survol, progression et accès détail discret.
- Catalogue filtrable par type et par genre via `/api/genres`.
- Route dédiée `#/my-list`.
- Fiche détail avec image de fond, poster, métadonnées, genres et épisodes.
- Lecteur HTML5 natif avec sous-titres WebVTT.
- Raccourcis clavier lecteur : `k`, espace, flèches, `m`, `f`.
- Streaming MP4 via endpoint Spring compatible HTTP Range.
- Sauvegarde de progression par utilisateur.
- PWA responsive sans dépendance frontend externe.
- Données et vidéos de démonstration générées localement, sans contenu protégé.

## V2 — UI plus épurée

Cette version ajoute une passe UI complémentaire : cartes silencieuses avec titre superposé, aperçu au survol, bouton détail triangulaire, logo seul en barre supérieure, icône de profil pour la déconnexion, opacité renforcée des boutons textuels et carrousels avec flèches conditionnelles + navigation circulaire.

## V3 — corrections rails, miniatures et aperçu au clic

Cette version corrige les problèmes d'espacement et de défilement induits par les aperçus intégrés aux cartes :

- suppression de l'aperçu au survol dans le flux des carrousels ;
- aperçu centré en modale fixe, déclenché uniquement par clic sur la miniature ;
- animation d'ouverture par fondu + agrandissement progressif ;
- clic sur le bouton lecture séparé du clic miniature ;
- sections plus rapprochées et descriptions de section masquées pour réduire le bruit visuel ;
- carrousels sans barre de défilement visible, avec navigation par flèches conditionnelles ;
- miniatures SVG régénérées sans titre ni sous-titre incrustés ;
- cache PWA incrémenté en `streamfolio-shell-v8`.

## V4 — régénération des ressources et cache de développement

Cette version ajoute une procédure explicite pour éviter de revoir les anciennes miniatures ou l'ancien catalogue après une itération UI.

### Régénérer les miniatures

```bash
python3 scripts/regenerate-posters.py
```

Sur Windows :

```powershell
py scripts\regenerate-posters.py
```

Les fichiers sont recréés dans :

```text
backend/src/main/resources/static/assets/posters/
```

Les miniatures SVG générées ne contiennent aucun texte visible. Vérification possible :

```bash
grep -R "<text\|<tspan" -n backend/src/main/resources/static/assets/posters
```

La commande ne doit rien retourner.

### Réinitialiser l'état local backend

```bash
./scripts/reset-dev-state.sh
cd backend
mvn clean spring-boot:run
```

Sur Windows :

```powershell
.\scripts\reset-dev-state.ps1
cd backend
mvn clean spring-boot:run
```

### Vider le cache navigateur/PWA

Si l'application affiche encore les anciennes miniatures, vider le cache du site `http://localhost:8080` :

```js
await navigator.serviceWorker.getRegistrations().then((items) => Promise.all(items.map((item) => item.unregister())));
await caches.keys().then((keys) => Promise.all(keys.map((key) => caches.delete(key))));
localStorage.clear();
location.reload();
```

Le service worker est en stratégie réseau d'abord pour les ressources statiques, et le cache PWA est incrémenté en `streamfolio-shell-v8`.

### Catalogue

Le seed de démonstration contient maintenant 18 titres catalogue. Certains titres réutilisent volontairement les mêmes fichiers MP4 courts pour garder le projet léger.

## V5 — corrections scrollbars, carrousels et miniatures propres

Cette version corrige les points suivants :

- stylisation globale des barres de défilement visibles : page, modale d'aperçu, rails de filtres ;
- conservation des carrousels sans scrollbar visible ;
- flèches de carrousel masquées par défaut et visibles uniquement au survol ou au focus du carrousel ;
- priorité visuelle du bouton lecture sur les cartes placées aux extrémités ;
- nouvelles miniatures générées dans `backend/src/main/resources/static/assets/posters-clean/` ;
- écrasement volontaire des anciennes miniatures dans `assets/posters/` pour éviter les anciens visuels ;
- chemins de miniatures versionnés en `?v6` côté seed Java ;
- mise à jour automatique des titres existants au démarrage, au lieu de ne créer que les titres manquants.

### Régénération complète des miniatures

```bash
python3 scripts/regenerate-posters.py
```

Windows :

```powershell
py scripts\regenerate-posters.py
```

La commande recrée :

```text
backend/src/main/resources/static/assets/posters-clean/*.svg
backend/src/main/resources/static/assets/posters/*.svg
```

Vérification :

```bash
grep -R "<text\|<tspan" -n backend/src/main/resources/static/assets/posters backend/src/main/resources/static/assets/posters-clean
```

Résultat attendu : aucune ligne.

### Si l'interface affiche encore les anciennes miniatures

Relancer avec nettoyage complet :

```bash
./scripts/reset-dev-state.sh
python3 scripts/regenerate-posters.py
cd backend
mvn clean spring-boot:run
```

Puis vider le cache côté navigateur :

```js
await navigator.serviceWorker.getRegistrations().then((items) => Promise.all(items.map((item) => item.unregister())));
await caches.keys().then((keys) => Promise.all(keys.map((key) => caches.delete(key))));
localStorage.clear();
location.reload();
```

## V6 — corrections détail et carrousels

Changements appliqués :

- flèches de carrousel maintenues au-dessus des cartes au survol des vidéos placées aux extrémités ;
- largeur des flèches latérales réduite pour ne pas gêner le bouton lecture central ;
- retrait des textes visibles `Sélection éditoriale` et de la note descriptive du hero ;
- bloc de découverte simplifié en `Explorer` ;
- page détail avec glassmorphisme derrière le contenu, hors navbar ;
- suppression du bouton `Accueil` dans la fiche détail ;
- bouton de liste raccourci en `Ma liste` ;
- suppression de la liste d'épisodes pour les films ;
- conservation de la liste d'épisodes uniquement pour les séries ;
- cache PWA incrémenté en `streamfolio-shell-v8`.

## V7 — navigation homogène

Les pages `Accueil`, `Films`, `Séries` et `Ma liste` partagent maintenant le même rendu : hero contextuel, section `Explorer`, puis rails horizontaux. Les anciennes vues de type grille catalogue, les compteurs de résultats, `Tous les titres` et l'entrée de navigation `Catalogue` ont été retirés de l'interface visible.

Le frontend met en cache les réponses `/api/sections` et `/api/genres` pendant la session afin de limiter les transitions avec écran de chargement entre les vues principales. Le cache PWA est incrémenté en `streamfolio-shell-v8`.
