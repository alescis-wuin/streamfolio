# Checklist de validation

Objectif : vérifier que le dépôt est propre, que les tests backend passent, que Redis porte les sessions runtime, que la protection CSRF fonctionne, que les rôles applicatifs sont cohérents, que le smoke test fonctionne en mode standard et que le mode `local-media` reste opérationnel avant de poursuivre les phases streaming avancées.

## 1. Validation complète automatisée

Depuis la racine :

```bash
bash scripts/validate.sh
```

Critères attendus :

- aucun fichier généré suivi par Git ;
- scripts shell syntaxiquement valides ;
- JavaScript applicatif et tests E2E syntaxiquement valides ;
- script Python compilable ;
- `mvn clean test` OK ;
- package Maven généré ;
- Redis disponible pour les sessions applicatives ;
- application démarrée en mode `classpath` ;
- `./scripts/smoke.sh` OK en mode `classpath` ;
- médias locaux préparés ;
- application démarrée en mode `local-media` ;
- `./scripts/smoke.sh` OK en mode `local-media`.

Les logs applicatifs sont écrits dans `build/validation-logs/`.

## 2. Vérification des fichiers générés suivis par Git

```bash
bash scripts/check-clean-tree.sh
```

La commande échoue si Git suit encore un fichier correspondant à `backend/target/`, `*.class`, `__pycache__/` ou `*.pyc`.

## 3. Validation applicative manuelle

Démarrage standard :

```bash
docker compose up -d redis
cd backend
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Démarrage média local :

```bash
docker compose up -d redis
bash scripts/prepare-local-media.sh backend/data/media
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

Dans un second terminal :

```bash
./scripts/smoke.sh
```

## 4. Validation sécurité minimale

À vérifier par tests ou manuellement :

- `GET /api/csrf` renvoie `token`, `headerName` et `parameterName` ;
- `POST /api/auth/login` sans CSRF renvoie `403` ;
- le JSON de login ne contient pas de jeton de session ;
- le cookie de session contient `HttpOnly` et `SameSite=Strict` ;
- la session est stockée dans Redis avec TTL et reste valide après redémarrage backend si Redis conserve la clé ;
- `/api/videos/1`, `/api/videos/1/stream`, `/api/videos/1/subtitles` et HLS refusent les requêtes sans cookie ;
- les mutations authentifiées sans CSRF renvoient `403` ;
- `/api/auth/logout` invalide la session courante ;
- `/api/admin/**` refuse les comptes sans rôle `ADMIN` ;
- `/h2-console/` n'est pas accessible hors profil `dev`.

## 5. Validation publication média

- `USER` connecté → `GET /api/videos/{draftId}` renvoie `404` ;
- `USER` connecté → `GET /api/videos/{draftId}/stream` renvoie `404` ;
- `USER` connecté → `GET /api/videos/{draftId}/hls/master.m3u8` renvoie `404` ;
- `USER` connecté → `GET /api/videos/{draftId}/subtitles` renvoie `404` ;
- `USER` connecté → `GET /api/videos/{draftId}/thumbnails/manifest.json` renvoie `404` ;
- `ADMIN` connecté → `GET /api/admin/videos/{draftId}/preview` renvoie `200` ;
- les previews admin exposent des URLs sous `/api/admin/videos/{draftId}/preview/**`, jamais sous les endpoints publics.

## 6. Validation catalogue paginé

- `GET /api/catalog?page=0&size=12` renvoie `items` et `pagination` ;
- `GET /api/catalog?query=botanical&type=SERIES&genre=Botanique&page=0&size=2` renvoie uniquement les séries attendues ;
- `size` supérieur à la limite configurée renvoie `400` ;
- `page` négatif renvoie `400`.

## 7. Validation admin paginée

- `GET /api/admin/videos?page=0&size=20` renvoie `items` et `pagination` ;
- les filtres `query`, `type` et `genre` sont appliqués côté base de données ;
- les IDs de sélection globale restent disponibles via `GET /api/admin/videos/ids` ;
- les utilisateurs sans rôle `ADMIN` reçoivent `403` sur tous les endpoints `/api/admin/**`.

## 8. Validation UI

Parcours recommandé : login, accueil, Films, Séries, recherche, fiche détail, watchlist, lecture, sélection manuelle de qualité HLS si HLS est disponible, sous-titres, reprise de progression, administration, logout, puis refus d'une URL vidéo directe sans session.

## 9. Validation profil production

- Le profil `prod` doit démarrer avec des cookies de session sécurisés ;
- une configuration production non sécurisée doit arrêter le démarrage ;
- le profil `prod` doit utiliser une base persistante, Redis durable et un stockage média externe ou persistant.

## 10. Validation E2E

```bash
npm install
npx playwright install chromium
npm run test:e2e
```

Ou :

```bash
bash scripts/e2e.sh
```

## 11. Validation CI

[![Dernière CI verte](https://github.com/alescis-wuin/streamfolio/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/alescis-wuin/streamfolio/actions/workflows/ci.yml?query=branch%3Amain)

La CI GitHub Actions exécute : installation Java/Node, installation `jq` et FFmpeg, `bash scripts/validate.sh`, tests E2E Playwright, publication des artifacts et build Docker.

Elle est déclenchée automatiquement sur push vers `main` ou `develop`, pull request vers `main` ou `develop`, et lancement manuel via `workflow_dispatch`.

## 12. Passage à la phase suivante

La phase suivante ne doit commencer que si :

- `bash scripts/validate.sh` passe localement ;
- `npm run test:e2e` passe localement ou en CI ;
- le parcours UI ne montre pas de régression bloquante ;
- la CI GitHub Actions est verte ;
- le README affiche les badges, le GIF ou un lien vers les captures documentées ;
- la documentation différencie clairement données PostgreSQL persistées et sessions runtime Redis.
