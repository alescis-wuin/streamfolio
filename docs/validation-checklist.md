# Checklist de validation

Objectif : vérifier que la couche sécurité, les tests, le smoke test, le parcours utilisateur et le stockage média local sont stables avant de passer au pipeline FFmpeg/HLS.

## 1. Validation backend

Depuis `backend/` :

```bash
mvn clean test
```

Critères attendus :

- compilation Java OK ;
- tests unitaires `AuthServiceTest` OK ;
- tests sécurité `SecurityIntegrationTest` OK ;
- tests catalogue `CatalogServiceIntegrationTest` OK ;
- tests stockage média `MediaStorageServiceTest` OK ;
- tests streaming local `StreamingLocalMediaIntegrationTest` OK ;
- contexte Spring chargé sans erreur.

## 2. Validation statique

Depuis `backend/` :

```bash
node --check src/main/resources/static/app.js
```

Depuis la racine :

```bash
bash -n scripts/*.sh
python3 -m py_compile scripts/regenerate-posters.py
```

Critères attendus :

- JavaScript syntaxiquement valide ;
- scripts shell valides ;
- script Python compilable.

## 3. Validation applicative classpath

Terminal 1 :

```bash
cd backend
mvn spring-boot:run
```

Terminal 2, depuis la racine :

```bash
./scripts/smoke.sh
```

Critères attendus :

- `/api/health` répond ;
- login OK ;
- cookie de session conservé par le smoke test ;
- `/api/me` répond après login ;
- sections, genres et catalogue répondent ;
- endpoint vidéo protégé accessible après login ;
- logout OK.

## 4. Validation applicative local-media

Depuis la racine :

```bash
bash scripts/prepare-local-media.sh
```

Terminal 1 :

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local-media
```

Terminal 2, depuis la racine :

```bash
./scripts/smoke.sh
```

Critères attendus : mêmes résultats que le mode classpath, mais les fichiers sont lus depuis `backend/data/media`.

## 5. Validation sécurité minimale

À vérifier manuellement ou par test HTTP :

- `/api/videos/1` sans cookie renvoie `401` ;
- `/api/videos/1/stream` sans cookie renvoie `401` ;
- `/api/videos/1/subtitles` sans cookie renvoie `401` ;
- `/api/videos/1/progress` sans cookie renvoie `401` ;
- le JSON de login ne contient pas de token ;
- le cookie de session contient `HttpOnly` et `SameSite=Strict` ;
- `/api/auth/logout` invalide la session.

## 6. Validation UI

Parcours recommandé dans le navigateur :

1. ouvrir `http://localhost:8080` ;
2. se connecter avec le compte de démonstration ;
3. vérifier l'accueil ;
4. ouvrir Films et Séries ;
5. chercher un titre ;
6. ouvrir une fiche détail ;
7. ajouter puis retirer un titre de Ma liste ;
8. lancer une vidéo ;
9. vérifier les sous-titres ;
10. vérifier la reprise de progression ;
11. se déconnecter ;
12. vérifier qu'une URL vidéo directe n'est plus accessible.

## 7. Validation CI

Sur GitHub Actions, la CI doit valider :

- checks shell ;
- check JavaScript ;
- check Python ;
- `mvn clean test` ;
- build JAR ;
- démarrage application ;
- smoke test HTTP ;
- build Docker.

## 8. Passage à la phase suivante

La phase FFmpeg/HLS ne doit commencer que si :

- `mvn clean test` est vert ;
- `mvn spring-boot:run` démarre sans erreur ;
- `./scripts/smoke.sh` passe en mode classpath ;
- `./scripts/smoke.sh` passe en mode local-media ;
- la CI GitHub Actions est verte ;
- le parcours UI ci-dessus ne montre pas de régression bloquante.
