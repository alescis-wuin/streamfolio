<div align="center">

# Streamfolio

**Plateforme de streaming vidéo de démonstration, inspirée des expériences Netflix-like, développée pour portfolio.**

[![CI](https://github.com/alescis-wuin/streamfolio/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/alescis-wuin/streamfolio/actions/workflows/ci.yml)
![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Dernier commit](https://img.shields.io/github/last-commit/alescis-wuin/streamfolio/main?label=dernier%20commit)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F)
![PWA](https://img.shields.io/badge/PWA-native-5A0FC8)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED)
![Git LFS](https://img.shields.io/badge/Git%20LFS-enabled-blue)

</div>

---

## Aperçu

![Démo Streamfolio](docs/record/demo.gif)

> [!NOTE]
> Le GIF ci-dessus est une démo longue stockée via Git LFS. Pour un README public, une version plus courte et plus légère pourra être ajoutée dans `docs/screenshots/demo.gif`.

### Visuels existants du catalogue

<table>
  <tr>
    <td width="25%">
      <strong>Aurora Drift</strong><br>
      <img src="backend/src/main/resources/static/assets/posters-clean/aurora-drift.svg" alt="Affiche Aurora Drift" width="100%">
    </td>
    <td width="25%">
      <strong>Botanical Cities</strong><br>
      <img src="backend/src/main/resources/static/assets/posters-clean/botanical-cities.svg" alt="Affiche Botanical Cities" width="100%">
    </td>
    <td width="25%">
      <strong>Silent Protocol</strong><br>
      <img src="backend/src/main/resources/static/assets/posters-clean/silent-protocol.svg" alt="Affiche Silent Protocol" width="100%">
    </td>
    <td width="25%">
      <strong>Kitchen Orbit</strong><br>
      <img src="backend/src/main/resources/static/assets/posters-clean/kitchen-orbit.svg" alt="Affiche Kitchen Orbit" width="100%">
    </td>
  </tr>
  <tr>
    <td width="25%">
      <strong>Glass Archive</strong><br>
      <img src="backend/src/main/resources/static/assets/posters-clean/glass-archive.svg" alt="Affiche Glass Archive" width="100%">
    </td>
    <td width="25%">
      <strong>Neon Orchard</strong><br>
      <img src="backend/src/main/resources/static/assets/posters-clean/neon-orchard.svg" alt="Affiche Neon Orchard" width="100%">
    </td>
    <td width="25%">
      <strong>Signal Garden</strong><br>
      <img src="backend/src/main/resources/static/assets/posters-clean/signal-garden.svg" alt="Affiche Signal Garden" width="100%">
    </td>
    <td width="25%">
      <strong>Pixel Greenhouse</strong><br>
      <img src="backend/src/main/resources/static/assets/posters-clean/pixel-greenhouse.svg" alt="Affiche Pixel Greenhouse" width="100%">
    </td>
  </tr>
</table>

<details>
<summary><strong>Captures d'écran prévues</strong></summary>

Les captures applicatives suivantes compléteront le README lorsque les fichiers seront ajoutés dans `docs/screenshots/` :

- [ ] `docs/screenshots/home.png` — accueil après connexion ;
- [ ] `docs/screenshots/detail.png` — fiche détail ;
- [ ] `docs/screenshots/player.png` — lecteur vidéo ;
- [ ] `docs/screenshots/mobile.png` — vue responsive mobile ;
- [ ] `docs/screenshots/demo.gif` — GIF court optimisé pour README public.

Consignes détaillées : [`docs/screenshots/README.md`](docs/screenshots/README.md).

</details>

## Objectif

Streamfolio démontre la réalisation d'une application complète de type streaming : authentification, catalogue films/séries, watchlist, progression de lecture, fiches détail, lecteur vidéo HTML5, streaming MP4 progressif et socle HLS local.

Le projet privilégie une architecture simple à lancer et à évaluer : un backend Spring Boot, une base H2 pour la démo, une interface PWA responsive sans build frontend obligatoire et des médias de démonstration libres de contenu protégé.

## Fonctionnalités principales

| Domaine | Fonctionnalités |
|---|---|
| Authentification | Spring Security, BCrypt, cookie `HttpOnly`, logout serveur, session courte |
| Catalogue | Films, séries, genres, recherche, filtres, rails horizontaux, hero immersif |
| UX streaming | Fiches détail, progression, watchlist, reprise de lecture, raccourcis clavier |
| Vidéo | Lecteur HTML5, sous-titres WebVTT, HTTP Range, fallback MP4 progressif |
| HLS local | Stockage local, génération HLS via FFmpeg/script, endpoint HLS protégé, fallback MP4 |
| Qualité | Tests Maven, tests sécurité, tests streaming, smoke tests, CI GitHub Actions |
| Portfolio | Documentation, captures prévues, GIF, Docker, Git LFS, validation automatisée |

## Stack

| Couche | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring MVC, Spring Security, Spring Data JPA, Bean Validation |
| Sécurité | BCrypt, cookie de session `HttpOnly`, `SameSite=Strict`, expiration configurable |
| Base de données | H2 en mémoire par défaut, profil persistant disponible |
| Frontend | HTML, CSS, JavaScript natif, PWA, Service Worker |
| Streaming | MP4 progressif, HTTP Range, WebVTT, HLS local, hls.js avec fallback MP4 |
| Média | Classpath par défaut, stockage local `backend/data/media`, Git LFS pour les fichiers lourds |
| Outillage | Maven, Docker, Docker Compose, Bash, PowerShell, FFmpeg optionnel |
| CI | GitHub Actions : validation complète, logs en artifact, build Docker |

## Structure du projet

```text
backend/                     API Spring Boot + frontend PWA servi en statique
backend/src/main/java/       Code Java backend
backend/src/main/resources/  Configuration, UI, médias et données de démonstration
backend/src/test/            Tests backend
frontend/                    Notes frontend
scripts/                     Scripts Linux/Windows, smoke tests, validation, HLS
docs/                        Architecture, roadmap, captures, validation, setup GitHub
.github/workflows/           CI GitHub Actions
```

<details>
<summary><strong>Parcours utilisateur couvert</strong></summary>

1. Connexion avec le compte de démonstration.
2. Chargement du catalogue.
3. Navigation dans les rails façon plateforme de streaming.
4. Recherche par titre, genre ou ambiance.
5. Ouverture d'une fiche détail.
6. Ajout/retrait de la watchlist.
7. Lancement d'une vidéo.
8. Sauvegarde automatique de la progression.
9. Reprise de lecture.
10. Déconnexion serveur.

</details>

## Démarrage rapide

### Prérequis

| Outil | Usage |
|---|---|
| Java 21+ | Exécution backend |
| Maven 3.9+ | Build et tests |
| `jq` | Smoke tests HTTP |
| Docker / Docker Compose | Lancement conteneurisé optionnel |
| FFmpeg | Transcodage HLS local optionnel |
| Git LFS | Récupération des médias versionnés |

```bash
git lfs install
git lfs pull
```

### Lancement local

```bash
cd backend
mvn spring-boot:run
```

Ouvrir ensuite :

```text
http://localhost:8080
```

Compte de démonstration :

```text
Email        : alexis@example.dev
Mot de passe : demo1234
```

### Lancement Docker

```bash
docker compose up --build
```

## Validation

Commande principale depuis la racine :

```bash
bash scripts/validate.sh
```

Cette commande vérifie :

- l'absence de fichiers générés suivis par Git : `backend/target/`, `*.class`, `__pycache__`, `*.pyc` ;
- la syntaxe des scripts shell ;
- la syntaxe JavaScript ;
- la compilation du script Python de génération des affiches ;
- `mvn clean test` ;
- le packaging Maven ;
- le démarrage applicatif en mode `classpath` ;
- `./scripts/smoke.sh` en mode `classpath` ;
- la préparation du stockage local ;
- le démarrage applicatif en mode `local-media` ;
- `./scripts/smoke.sh` en mode `local-media`.

Vérification ciblée des fichiers générés suivis par Git :

```bash
bash scripts/check-clean-tree.sh
```

Les logs de validation sont écrits dans :

```text
build/validation-logs/
```

## Streaming local et HLS

Mode par défaut : médias servis depuis le classpath.

Mode local : médias lus depuis `backend/data/media`.

```bash
bash scripts/prepare-local-media.sh backend/data/media
```

Transcodage HLS local de démonstration :

```bash
bash scripts/transcode-demo-hls.sh 1 aurora-drift.mp4
```

Sortie attendue :

```text
backend/data/media/hls/1/master.m3u8
```

Lancer ensuite le backend en mode local-media :

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local-media
```

Le lecteur choisit HLS quand `master.m3u8` existe, sinon il retombe automatiquement sur le MP4 progressif.

## Médias et Git LFS

Les médias lourds sont suivis avec Git LFS via `.gitattributes`.

| Zone | Statut |
|---|---|
| `backend/src/main/resources/media/*.mp4` | Suivi LFS |
| `docs/record/*.mp4`, `*.mkv`, `*.gif`, `*.webm` | Suivi LFS |
| `docs/screenshots/*.png`, `*.gif`, `*.mp4`, `*.webm` | Suivi LFS |
| `backend/data/media/**` | Stockage local généré, non versionné |

## Qualité technique

| Contrôle | Couverture actuelle |
|---|---|
| Tests backend | Chargement contexte, auth, sécurité, catalogue, streaming, stockage média, FFmpeg/transcodage |
| Smoke tests | Health, login, cookie, `/api/me`, sections, genres, catalogue, streaming Range, logout |
| Sécurité | Endpoints vidéo protégés, cookie `HttpOnly`, absence de token dans le JSON login |
| CI | Validation complète + build Docker |
| Dépôt | `.gitignore`, `.dockerignore`, `.gitattributes`, Git LFS, contrôle des fichiers générés |

## Documentation

| Document | Rôle |
|---|---|
| [`docs/02-architecture.md`](docs/02-architecture.md) | Architecture générale |
| [`docs/04-roadmap.md`](docs/04-roadmap.md) | Roadmap fonctionnelle |
| [`docs/06-stockage-media.md`](docs/06-stockage-media.md) | Mode local, HLS et FFmpeg |
| [`docs/06-verification.md`](docs/06-verification.md) | Validation locale et CI |
| [`docs/validation-checklist.md`](docs/validation-checklist.md) | Checklist de passage de phase |
| [`docs/screenshots/README.md`](docs/screenshots/README.md) | Captures et GIF |

## Limites assumées

> [!IMPORTANT]
> Cette version est conçue pour un portfolio, pas pour une production réelle.

- Compte utilisateur de démonstration préchargé.
- Sessions applicatives en mémoire.
- Cookie `Secure` désactivé en local HTTP, activable derrière HTTPS.
- Base H2 par défaut.
- Pas de rôles utilisateur avancés.
- Pas de DRM.
- Pas de stockage objet S3/MinIO.
- Pas de CDN.
- HLS local démonstratif, sans pipeline complet d'administration/upload.
- Médias de démonstration courts et locaux.

## Roadmap

### Court terme

- Ajouter les captures statiques finales dans `docs/screenshots/`.
- Remplacer le GIF long par un GIF court optimisé pour README public.
- Stabiliser la phase HLS locale.
- Découper progressivement `app.js` et `styles.css`.
- Ajouter OpenAPI/Swagger.

### Moyen terme

- Ajouter PostgreSQL en profil Docker.
- Ajouter une page admin pour gérer titres, vidéos et genres.
- Ajouter un pipeline FFmpeg complet : upload, transcodage, posters, thumbnails, variantes vidéo.
- Ajouter HLS avec qualité adaptative.
- Ajouter des profils utilisateur.

### Long terme

- Stockage objet MinIO/S3.
- Recommandations personnalisées.
- Observabilité avec Spring Actuator, Prometheus et Grafana.
- Audit accessibilité WCAG.
- Déploiement cloud avec CDN.

## Historique

L'historique détaillé des versions V1 à V7 est disponible dans [`CHANGELOG.md`](CHANGELOG.md).
