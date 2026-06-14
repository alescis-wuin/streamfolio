# Checklist de validation

Objectif : vérifier que le dépôt est propre, que les tests backend passent, que le smoke test fonctionne en mode standard et que le mode `local-media` reste opérationnel avant de poursuivre les phases streaming avancées.

## 1. Validation complète automatisée

Depuis la racine :

```bash
bash scripts/validate.sh
```

Critères attendus :

- aucun fichier généré suivi par Git ;
- scripts shell syntaxiquement valides ;
- JavaScript syntaxiquement valide ;
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

- `/api/videos/1` sans cookie renvoie `401` ;
- `/api/videos/1/stream` sans cookie renvoie `401` ;
- `/api/videos/1/subtitles` sans cookie renvoie `401` ;
- `/api/videos/1/progress` sans cookie renvoie `401` ;
- le JSON de login ne contient pas de token ;
- le cookie de session contient `HttpOnly` et `SameSite=Strict` ;
- `/api/auth/logout` invalide la session.

## 5. Validation UI

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

## 6. Validation CI

La CI GitHub Actions exécute :

- installation Java ;
- installation Node.js ;
- installation `jq` ;
- `bash scripts/validate.sh` ;
- publication de l'artifact `validation-logs` ;
- build Docker.

La CI est déclenchée automatiquement sur :

- push vers `main` ou `develop` ;
- pull request vers `main` ou `develop` ;
- lancement manuel via `workflow_dispatch`.

## 7. Passage à la phase suivante

La phase suivante ne doit commencer que si :

- `bash scripts/validate.sh` passe localement ;
- le parcours UI ne montre pas de régression bloquante ;
- la CI GitHub Actions est verte ;
- les captures `home.png`, `detail.png`, `player.png`, `mobile.png` et `demo.gif` sont ajoutées ou explicitement reportées.
