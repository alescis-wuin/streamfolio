# Vérification

## Commande principale

Depuis la racine du dépôt :

```bash
bash scripts/validate.sh
```

Cette commande automatise la validation complète du projet :

- contrôle des fichiers générés suivis par Git ;
- contrôle syntaxique des scripts shell ;
- contrôle syntaxique JavaScript, dont `csrf.js`, `playwright.config.js` et les tests `tests/e2e` ;
- compilation du script Python ;
- `mvn clean test` ;
- packaging Maven ;
- démarrage en mode `classpath` ;
- smoke test HTTP en mode `classpath`, avec récupération du jeton CSRF ;
- préparation du stockage média local ;
- démarrage en mode `local-media` ;
- smoke test HTTP en mode `local-media`, avec récupération du jeton CSRF.

Les logs applicatifs sont produits dans :

```text
build/validation-logs/
```

## Vérification des fichiers générés

```bash
bash scripts/check-clean-tree.sh
```

La commande échoue si un fichier généré est encore suivi par Git :

```text
backend/target/
*.class
__pycache__/
*.pyc
```

## Validation CSRF

L'application expose un endpoint public de récupération du jeton :

```bash
curl -c cookies.txt -b cookies.txt http://localhost:8080/api/csrf
```

Les requêtes mutantes doivent ensuite fournir le header retourné par cet endpoint, par défaut :

```text
X-XSRF-TOKEN: <token>
```

Sont notamment protégés :

- `POST /api/auth/login` ;
- `POST /api/auth/logout` ;
- `PUT /api/videos/{videoId}/progress` ;
- `POST /api/titles/{titleId}/watchlist` ;
- `DELETE /api/titles/{titleId}/watchlist`.

## Validation E2E Playwright

Installation puis exécution :

```bash
npm install
npx playwright install chromium
npm run test:e2e
```

Ou via le script dédié :

```bash
bash scripts/e2e.sh
```

Le rapport HTML est généré dans :

```text
build/playwright-report/
```

## Validation CI

Le workflow `.github/workflows/ci.yml` lance `bash scripts/validate.sh`, publie les logs de validation en artifact GitHub Actions, exécute les tests Playwright, publie le rapport Playwright, puis construit l'image Docker.

Déclenchements :

- push vers `main` ou `develop` ;
- pull request vers `main` ou `develop` ;
- lancement manuel depuis l'onglet GitHub Actions.

## Vérification manuelle minimale

```bash
cd backend
mvn spring-boot:run
```

Dans un second terminal :

```bash
./scripts/smoke.sh
```

Mode `local-media` :

```bash
bash scripts/prepare-local-media.sh backend/data/media
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local-media
```

Dans un second terminal :

```bash
./scripts/smoke.sh
```

## Critères avant phase suivante

- validation locale complète verte ;
- CI verte ;
- tests Playwright verts ;
- parcours UI manuel sans régression bloquante ;
- captures portfolio ajoutées dans `docs/screenshots/`.
