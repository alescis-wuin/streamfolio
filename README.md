# Streamfolio

Plateforme de streaming vidéo de démonstration pour portfolio, développée avec Java, Spring Boot et une PWA native servie par le backend.

## Objectif

Streamfolio montre la réalisation d'une application complète de type streaming : authentification, catalogue films/séries, watchlist, progression de lecture, fiches détail et lecteur vidéo HTML5 avec streaming MP4 progressif.

Le projet privilégie une architecture simple à lancer et à évaluer : un backend Spring Boot, une base H2 pour la démo, une interface responsive sans build frontend obligatoire et des médias de démonstration libres de contenu protégé.

## Stack

| Couche | Technologies |
|---|---|
| Backend | Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA, Bean Validation |
| Sécurité | BCrypt, cookie de session HttpOnly, SameSite Strict, expiration configurable |
| Base de données | H2 en mémoire par défaut, profil persistant disponible |
| Frontend | HTML, CSS, JavaScript natif, PWA, Service Worker |
| Vidéo | Lecteur HTML5, MP4 progressif, requêtes HTTP Range, sous-titres WebVTT |
| Outillage | Maven, Docker, Docker Compose, scripts Bash/PowerShell |
| CI | GitHub Actions : tests Maven, checks statiques, smoke test HTTP, build Docker |

## Fonctionnalités

- Authentification de démonstration avec Spring Security.
- Mot de passe de démonstration encodé avec BCrypt.
- Session applicative courte via cookie HttpOnly.
- Endpoint de logout serveur.
- Catalogue de films et séries avec genres, recherche et filtres.
- Accueil avec hero immersif et rails horizontaux.
- Pages dédiées : Accueil, Films, Séries, Ma liste.
- Fiche détail avec métadonnées, genres, affiche, synopsis et épisodes pour les séries.
- Watchlist utilisateur.
- Sauvegarde de progression par vidéo.
- Lecteur HTML5 avec sous-titres WebVTT.
- Raccourcis clavier du lecteur : `k`, espace, flèches, `m`, `f`.
- Streaming MP4 via endpoint Spring compatible HTTP Range.
- Endpoints vidéo protégés : metadata, stream, sous-titres et progression.
- PWA responsive utilisable sans dépendance frontend externe.
- Données, affiches SVG et vidéos de démonstration incluses localement.

## Captures d'écran

Les captures sont à placer dans `docs/screenshots/`.

Captures recommandées :

| Fichier | Contenu |
|---|---|
| `docs/screenshots/home.png` | Accueil après connexion, avec hero et rails visibles |
| `docs/screenshots/detail.png` | Fiche détail d'un film ou d'une série |
| `docs/screenshots/player.png` | Lecteur vidéo en cours de lecture |
| `docs/screenshots/mobile.png` | Vue responsive mobile |
| `docs/screenshots/demo.gif` | Parcours court : login, recherche, détail, lecture, watchlist |

Quand les fichiers sont ajoutés, insérer par exemple :

```md
![Accueil Streamfolio](docs/screenshots/home.png)
![Lecteur vidéo](docs/screenshots/player.png)
```

Les consignes détaillées sont dans [`docs/screenshots/README.md`](docs/screenshots/README.md).

## Démarrage

### Prérequis

- Java 21+
- Maven 3.9+
- Navigateur moderne
- `jq` pour le smoke test HTTP
- Docker et Docker Compose, optionnels

Installation de `jq` :

```bash
# Debian / Ubuntu
sudo apt install jq

# Fedora
sudo dnf install jq

# Arch Linux
sudo pacman -S jq

# macOS avec Homebrew
brew install jq

# Windows avec winget
winget install jqlang.jq
```

### Lancer localement

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

### Lancer avec Docker

```bash
docker compose up --build
```

### Base persistante locale

Par défaut, l'application utilise H2 en mémoire. La progression et la watchlist sont donc réinitialisées à chaque lancement.

Pour conserver l'état localement :

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=persistent
```

### Console H2 locale

La console H2 est désactivée par défaut. Pour l'activer explicitement en développement :

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Réinitialisation de l'état local :

```bash
./scripts/reset-dev-state.sh
```

Sur Windows :

```powershell
.\scripts\reset-dev-state.ps1
```

## Vérification

```bash
cd backend
mvn clean test
```

```bash
node --check src/main/resources/static/app.js
```

Depuis la racine :

```bash
bash -n scripts/*.sh
python3 -m py_compile scripts/regenerate-posters.py
```

Dans un autre terminal, une fois l'application lancée :

```bash
./scripts/smoke.sh
```

Le smoke test vérifie notamment :

- `/api/health` ;
- login de démonstration ;
- cookie de session ;
- `/api/me` ;
- sections du catalogue ;
- genres ;
- catalogue filtré ;
- endpoint vidéo protégé avec header HTTP `Range`.

Checklist complète : [`docs/validation-checklist.md`](docs/validation-checklist.md).

## Structure

```text
backend/                     API Spring Boot + frontend PWA servi en statique
backend/src/main/java/       Code Java backend
backend/src/main/resources/  Configuration, UI, médias et données de démo
backend/src/test/            Tests backend
frontend/                    Notes frontend
scripts/                     Scripts Linux/Windows et smoke test HTTP
docs/                        Architecture, recherche, roadmap, captures, validation, setup GitHub
.github/workflows/           CI GitHub Actions
```

## Repository GitHub

Le connecteur disponible ici ne permet pas de créer directement un nouveau repository. Les commandes de création sont documentées dans [`docs/github/repository-setup.md`](docs/github/repository-setup.md).

Commande recommandée avec GitHub CLI :

```bash
git init
git add .
git commit -m "Initial commit: Streamfolio"
gh repo create streamfolio --private --source=. --remote=origin --push
```

## Limites assumées

Cette version est conçue pour un portfolio, pas pour une production réelle.

Limites principales :

- compte utilisateur de démonstration préchargé ;
- sessions applicatives en mémoire ;
- cookie Secure désactivé en local HTTP, activable derrière HTTPS ;
- base H2 par défaut ;
- pas de rôles utilisateur avancés ;
- pas de DRM ;
- pas de stockage objet S3/MinIO ;
- pas de transcodage FFmpeg ;
- pas de streaming HLS/DASH adaptatif ;
- pas de CDN ;
- médias de démonstration courts et locaux.

## Roadmap

### Court terme

- Stabiliser la phase 2.5 : tests, smoke test, CI et validation UI.
- Découper progressivement `app.js` et `styles.css`.
- Ajouter OpenAPI/Swagger.
- Ajouter PostgreSQL en profil Docker.

### Moyen terme

- Ajouter une page admin pour gérer titres, vidéos et genres.
- Ajouter un pipeline FFmpeg pour générer affiches, thumbnails et variantes vidéo.
- Ajouter HLS avec qualité adaptative.
- Ajouter des profils utilisateur.

### Long terme

- Stockage objet MinIO/S3.
- Recommandations personnalisées.
- Observabilité avec Spring Actuator, Prometheus et Grafana.
- Audit accessibilité WCAG.
- Déploiement cloud avec CDN.

## Historique

L'historique détaillé des versions V1 à V7 a été déplacé dans [`CHANGELOG.md`](CHANGELOG.md).
