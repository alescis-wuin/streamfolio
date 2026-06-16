# Persistance PostgreSQL

## Objectif

Le profil par défaut de développement utilise H2 en mémoire et `ddl-auto=create-drop`. Il est utile pour une démo jetable, mais il détruit le catalogue ajouté, la progression, la watchlist et les jobs à chaque arrêt applicatif.

Les profils `docker` et `postgres` utilisent PostgreSQL, Flyway et un volume Docker nommé `postgres-data`.

## Données persistées

| Domaine | Tables |
| --- | --- |
| Catalogue | `catalog_titles`, `catalog_title_genres`, `catalog_videos` |
| Médias | `media_assets` |
| Jobs | `transcode_jobs` |
| Utilisateurs | `user_accounts`, `user_account_roles` |
| Expérience utilisateur | `watchlist_items`, `user_progress` |

Les liaisons films/séries/saisons sont portées par `catalog_titles` et `catalog_videos` avec `title_id`, `season_number` et `episode_number`.

## Sessions applicatives

PostgreSQL persiste les données métier et les rôles, mais il ne persiste pas automatiquement les sessions applicatives.

Dans l'implémentation actuelle, le jeton de session émis à la connexion est conservé dans le processus backend. Conséquence : après redémarrage du backend, l'utilisateur doit se reconnecter. Cela ne supprime pas les comptes, rôles, vidéos, métadonnées, watchlist, progression ou jobs stockés dans PostgreSQL.

Pour une cible production, remplacer ce stockage runtime par Spring Session JDBC/Redis, ou un autre mécanisme de session partagé et persistant.

## Rôles

Les rôles applicatifs sont stockés dans `user_account_roles`.

| Rôle | Usage |
| --- | --- |
| `USER` | Consultation catalogue, lecture, watchlist, progression |
| `ADMIN` | Accès aux routes `/api/admin/**` et aux workflows média |

Le compte de démonstration est promu en `ADMIN` au démarrage afin de conserver le parcours portfolio sans création manuelle de compte.

## Lancement Docker durable

```bash
cp .env.example .env
docker compose up --build
```

À l'arrêt simple, les données restent dans le volume Docker :

```bash
docker compose stop
docker compose start
```

La commande suivante supprime explicitement les données PostgreSQL et doit être évitée si l'objectif est la persistance :

```bash
docker compose down -v
```

## PostgreSQL 18 et volume Docker

L'image officielle PostgreSQL 18 utilise un répertoire de données versionné sous `/var/lib/postgresql`, par exemple `/var/lib/postgresql/18/docker`. Le volume Compose doit donc être monté sur `/var/lib/postgresql`, pas sur l'ancien chemin `/var/lib/postgresql/data`.

Si un premier démarrage a échoué avec l'ancien montage et qu'aucune donnée réelle n'a encore été créée, réinitialiser uniquement les conteneurs/volumes de développement :

```bash
docker compose down --remove-orphans -v
docker compose up -d
```

Ne pas utiliser `-v` sur une base contenant déjà des données à conserver.

## FFmpeg et miniatures Docker

Le backend Docker installe `ffmpeg` et `ffprobe` dans l'image runtime. Ils sont requis pour :

- extraire une miniature depuis une vidéo uploadée avec un timestamp ;
- lire les métadonnées/durées via `ffprobe` ;
- transcoder les vidéos locales en HLS ;
- générer les thumbnails de timeline HLS.

Vérification dans le conteneur :

```bash
docker compose exec streamfolio ffmpeg -version
docker compose exec streamfolio ffprobe -version
```

## Lancement Maven avec PostgreSQL local/Docker

Démarrer uniquement PostgreSQL :

```bash
docker compose up -d postgres
```

Puis lancer le backend avec le profil PostgreSQL :

```bash
cd backend
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```

Pour un lancement distant derrière HTTPS :

```bash
SPRING_PROFILES_ACTIVE=distant,postgres \
STREAMFOLIO_COOKIE_SECURE=true \
mvn spring-boot:run
```

La variable correcte est `STREAMFOLIO_COOKIE_SECURE`. Elle force l'attribut `Secure` du cookie applicatif.

## Migrations

Les migrations sont stockées dans :

```text
backend/src/main/resources/db/migration/postgresql/
```

Au démarrage des profils `postgres` et `docker` :

1. Flyway applique les migrations manquantes.
2. Hibernate valide le schéma avec `ddl-auto=validate`.
3. Le chargeur de démonstration ajoute ou rafraîchit les données initiales de manière idempotente.

## Variables utiles

| Variable | Défaut Docker |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `docker` |
| `STREAMFOLIO_POSTGRES_URL` | `jdbc:postgresql://postgres:5432/streamfolio` |
| `STREAMFOLIO_POSTGRES_USER` | `streamfolio` |
| `STREAMFOLIO_POSTGRES_PASSWORD` | `change-me` |
| `STREAMFOLIO_MEDIA_ROOT` | `/app/data/media` |
| `STREAMFOLIO_COOKIE_SECURE` | `false` en local, `true` recommandé derrière HTTPS |
| `STREAMFOLIO_FFMPEG_BINARY` | `/usr/bin/ffmpeg` |
| `STREAMFOLIO_FFMPEG_PROBE_BINARY` | `/usr/bin/ffprobe` |

## Vérification manuelle

1. Démarrer `docker compose up --build`.
2. Se connecter avec le compte de démonstration.
3. Vérifier que `/api/me` expose les rôles attendus.
4. Ajouter un titre à la liste.
5. Regarder une vidéo quelques secondes.
6. Uploader une vidéo depuis l'administration.
7. Extraire une miniature depuis la vidéo uploadée avec un timestamp.
8. Lancer un transcodage HLS.
9. Arrêter avec `docker compose stop`.
10. Redémarrer avec `docker compose start`.
11. Vérifier que la liste, la progression, les métadonnées, l'upload et les jobs sont toujours présents.
12. Vérifier qu'une reconnexion est nécessaire après redémarrage si les sessions ne sont pas encore externalisées.

## Sauvegarde rapide

```bash
mkdir -p backups
docker compose exec -T postgres pg_dump -U streamfolio -d streamfolio > backups/streamfolio.sql
```

## Restauration rapide

```bash
cat backups/streamfolio.sql | docker compose exec -T postgres psql -U streamfolio -d streamfolio
```
