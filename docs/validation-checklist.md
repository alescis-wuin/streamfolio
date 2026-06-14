# Checklist de validation

Objectif : vérifier que le dépôt est propre, que les tests backend passent, que la protection CSRF fonctionne, que le smoke test fonctionne en mode standard et que le mode `local-media` reste opérationnel avant de poursuivre les phases streaming avancées.

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
- application démarrée en mode `classpath` ;
- `./scripts/smoke.sh` OK en mode `classpath` ;
- médias locaux préparés ;
- application démarrée en mode `local-media` ;
- `./scripts/smoke.sh` OK en mode `local-media`.

Les logs applicatifs sont écrits dans :

```text
build/validation-logs/
```

## 2. Vérification des fichiers générés suivis par Git

Depuis la racine :

```bash
bash scripts/check-clean-tree.sh
```

La commande échoue si Git suit encore un fichier correspondant à :

```text
backend/target/
*.class
__pycache__/
*.pyc
```

Si la commande échoue, corriger avec :

```bash
git rm -r --cached backend/target '**/*.class' '**/__pycache__' '**/*.pyc'
git commit -m "Remove generated files from repository"
```

## 3. Validation applicative manuelle

Démarrage standard :

```bash
cd backend
mvn spring-boot:run
```

Dans un second terminal :

```bash
./scripts/smoke.sh
```

Démarrage `local-media` :

```bash
bash scripts/prepare-local-media.sh backend/data/media
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local-media
```

Dans un second terminal :

```bash
./scripts/smoke.sh
```

## 4. Validation sécurité minimale

À vérifier par tests ou manuellement :

- `GET /api/csrf` renvoie `token`, `headerName` et `parameterName` ;
- `POST /api/auth/login` sans CSRF renvoie `403` ;
- `PUT /api/videos/1/progress` avec session mais sans CSRF renvoie `403` ;
- `/api/videos/1` sans cookie renvoie `401` ;
- `/api/videos/1/stream` sans cookie renvoie `401` ;
- `/api/videos/1/subtitles` sans cookie renvoie `401` ;
- `/api/videos/1/progress` sans cookie renvoie `401` si le CSRF est fourni ;
- le JSON de login ne contient pas de token ;
- le cookie de session contient `HttpOnly` et `SameSite=Strict` ;
- `/api/auth/logout` invalide la session ;
- `/h2-console/` n'est pas accessible hors profil `dev` ;
- `/h2-console/` est accessible avec le profil `dev`.

## 5. Validation catalogue paginé

À vérifier par tests ou manuellement :

- `GET /api/catalog?page=0&size=12` renvoie `items` et `pagination` ;
- `GET /api/catalog?query=botanical&type=SERIES&genre=Botanique&page=0&size=2` renvoie uniquement des séries contenant le genre `Botanique` ;
- `size` supérieur à la limite configurée renvoie `400` ;
- `page` négatif renvoie `400`.

## 6. Validation UI

Parcours recommandé dans le navigateur :

1. ouvrir `http://localhost:8080` ;
2. se connecter avec `alexis@example.dev / demo1234` ;
3. vérifier l'accueil ;
4. ouvrir Films et Séries ;
5. chercher `botanical` ;
6. ouvrir une fiche détail ;
7. ajouter puis retirer un titre de Ma liste ;
8. lancer une vidéo ;
9. vérifier les sous-titres ;
10. vérifier la reprise de progression ;
11. se déconnecter ;
12. vérifier qu'une URL vidéo directe n'est plus accessible.

## 7. Validation E2E

Depuis la racine :

```bash
npm install
npx playwright install chromium
npm run test:e2e
```

Ou :

```bash
bash scripts/e2e.sh
```

Les tests couvrent :

- login ;
- recherche ;
- fiche détail ;
- watchlist ;
- lecteur ;
- logout ;
- refus des endpoints vidéo anonymes ;
- pagination/filtrage API ;
- refus des mutations sans CSRF.

## 8. Validation CI

La CI GitHub Actions exécute :

- installation Java ;
- installation Node.js ;
- installation `jq` ;
- `bash scripts/validate.sh` ;
- installation des navigateurs Playwright ;
- tests E2E Playwright ;
- publication des artifacts `validation-logs` et `playwright-report` ;
- build Docker.

La CI est déclenchée automatiquement sur :

- push vers `main` ou `develop` ;
- pull request vers `main` ou `develop` ;
- lancement manuel via `workflow_dispatch`.

## 9. Passage à la phase suivante

La phase suivante ne doit commencer que si :

- `bash scripts/validate.sh` passe localement ;
- `npm run test:e2e` passe localement ou en CI ;
- le parcours UI ne montre pas de régression bloquante ;
- la CI GitHub Actions est verte ;
- les captures `home.png`, `detail.png`, `player.png`, `mobile.png` et `demo.gif` sont ajoutées ou explicitement reportées.
