# Persistance PostgreSQL et sessions Redis

## Objectif

PostgreSQL persiste les données métier : utilisateurs, rôles, catalogue, médias, jobs, watchlist et progression. Redis stocke les sessions runtime afin d'éviter de dépendre de la mémoire du processus backend.

## Données persistées

| Domaine | Tables |
| --- | --- |
| Catalogue | `catalog_titles`, `catalog_title_genres`, `catalog_videos` |
| Médias | `media_assets` |
| Jobs | `transcode_jobs` |
| Utilisateurs | `user_accounts`, `user_account_roles` |
| Expérience utilisateur | `watchlist_items`, `user_progress` |

## Sessions applicatives

Les sessions sont écrites dans Redis sous le préfixe `streamfolio:sessions:` avec un TTL aligné sur `streamfolio.security.session-ttl`. Le logout supprime la clé Redis. Un redémarrage backend ne supprime donc plus les sessions si Redis conserve les clés.

Le Redis Compose est volontairement simple et sans persistance disque. Pour une production réelle, prévoir Redis managé ou hautement disponible, secrets hors dépôt et rotation adaptée.

## Lancement local

```bash
docker compose up -d postgres redis
cd backend
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```

## Variables utiles

| Variable | Défaut Docker |
| --- | --- |
| `STREAMFOLIO_POSTGRES_URL` | `jdbc:postgresql://postgres:5432/streamfolio` |
| `STREAMFOLIO_POSTGRES_USER` | `streamfolio` |
| `STREAMFOLIO_POSTGRES_PASSWORD` | `change-me` |
| `STREAMFOLIO_REDIS_URL` | `redis://redis:6379` |
| `STREAMFOLIO_SESSION_KEY_PREFIX` | `streamfolio:sessions:` |
| `STREAMFOLIO_COOKIE_SECURE` | `false` en local, `true` recommandé derrière HTTPS |

## Vérification manuelle

1. Démarrer `docker compose up --build`.
2. Se connecter avec le compte de démonstration.
3. Vérifier `/api/me`.
4. Redémarrer uniquement le backend en gardant Redis actif.
5. Vérifier que `/api/me` reste accessible jusqu'à expiration de session.
6. Vérifier que watchlist, progression, médias, uploads et jobs restent portés par PostgreSQL.
