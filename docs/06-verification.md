# Vérification

## Commande principale

Depuis la racine du dépôt :

```bash
bash scripts/validate.sh
```

Cette commande automatise la validation complète du projet :

- contrôle des fichiers générés suivis par Git ;
- contrôle syntaxique des scripts shell ;
- contrôle syntaxique JavaScript ;
- compilation du script Python ;
- `mvn clean test` ;
- packaging Maven ;
- démarrage en mode `classpath` ;
- smoke test HTTP en mode `classpath` ;
- préparation du stockage média local ;
- démarrage en mode `local-media` ;
- smoke test HTTP en mode `local-media`.

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

## Validation CI

Le workflow `.github/workflows/ci.yml` lance `bash scripts/validate.sh`, publie les logs de validation en artifact GitHub Actions, puis construit l'image Docker.

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
- parcours UI manuel sans régression bloquante ;
- captures portfolio ajoutées dans `docs/screenshots/`.
