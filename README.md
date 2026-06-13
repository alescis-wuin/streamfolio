# Streamfolio

Plateforme de streaming vidéo de démonstration pour portfolio, développée avec Java, Spring Boot et une PWA native servie par le backend.

## Objectif

Streamfolio montre la réalisation d'une application complète de type streaming : authentification de démonstration, catalogue films/séries, watchlist, progression de lecture, fiches détail et lecteur vidéo HTML5 avec streaming MP4 progressif.

Le projet privilégie une architecture simple à lancer et à évaluer : un backend Spring Boot, une base H2 pour la démo, une interface responsive sans build frontend obligatoire et des médias de démonstration libres de contenu protégé.

## Stack

| Couche | Technologies |
|---|---|
| Backend | Java 21, Spring Boot, Spring MVC, Spring Data JPA, Bean Validation |
| Base de données | H2 en mémoire par défaut, profil persistant disponible |
| Frontend | HTML, CSS, JavaScript natif, PWA, Service Worker |
| Vidéo | Lecteur HTML5, MP4 progressif, requêtes HTTP Range, sous-titres WebVTT |
| Outillage | Maven, Docker, Docker Compose, scripts Bash/PowerShell |
| CI | GitHub Actions : tests Maven, checks statiques, smoke test HTTP, build Docker |

## Fonctionnalités

- Authentification de démonstration avec compte préchargé.
- Catalogue de films et séries avec genres, recherche et filtres.
- Accueil avec hero immersif et rails horizontaux.
- Pages dédiées : Accueil, Films, Séries, Ma liste.
- Fiche détail avec métadonnées, genres, affiche, synopsis et épisodes pour les séries.
- Watchlist utilisateur.
- Sauvegarde de progression par vidéo.
- Lecteur HTML5 avec sous-titres WebVTT.
- Raccourcis clavier du lecteur : `k`, espace, flèches, `m`, `f`.
- Streaming MP4 via endpoint Spring compatible HTTP Range.
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
# Tests backend
cd backend
mvn test
```

```bash
# Build JAR
cd backend
mvn -DskipTests package
java -jar target/streamfolio-v1-1.0.0.jar
```

Dans un autre terminal, une fois l'application lancée :

```bash
./scripts/smoke.sh
```

Le smoke test vérifie notamment :

- `/api/health` ;
- login de démonstration ;
- `/api/me` ;
- sections du catalogue ;
- genres ;
- catalogue filtré ;
- endpoint vidéo avec header HTTP `Range`.

## Structure

```text
backend/                     API Spring Boot + frontend PWA servi en statique
backend/src/main/java/       Code Java backend
backend/src/main/resources/  Configuration, UI, médias et données de démo
backend/src/test/            Tests backend
frontend/                    Notes frontend
scripts/                     Scripts Linux/Windows et smoke test HTTP
docs/                        Architecture, recherche, roadmap, captures, setup GitHub
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

- authentification volontairement simplifiée ;
- sessions en mémoire ;
- mot de passe de démonstration ;
- base H2 par défaut ;
- pas de Spring Security complet ;
- pas de rôles utilisateur ;
- pas de DRM ;
- pas de stockage objet S3/MinIO ;
- pas de transcodage FFmpeg ;
- pas de streaming HLS/DASH adaptatif ;
- pas de CDN ;
- médias de démonstration courts et locaux.

## Roadmap

### Court terme

- Ajouter Spring Security.
- Remplacer le hash de démonstration par BCrypt ou Argon2.
- Protéger les endpoints de streaming et de sous-titres.
- Ajouter des tests MockMvc sur les principaux endpoints.
- Ajouter des tests unitaires sur les services métier.
- Optimiser les requêtes de catalogue pour éviter les traitements en mémoire et les risques de N+1.
- Découper progressivement `app.js` et `styles.css`.

### Moyen terme

- Ajouter PostgreSQL en profil Docker.
- Ajouter OpenAPI/Swagger.
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
