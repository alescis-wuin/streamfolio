# Création du repository GitHub

Le dépôt recommandé pour ce projet est `streamfolio`.

## Création avec GitHub CLI

Depuis la racine du projet :

```bash
git init
git add .
git commit -m "Initial commit: Streamfolio"
gh repo create streamfolio --private --source=. --remote=origin --push
```

Pour un dépôt public :

```bash
gh repo create streamfolio --public --source=. --remote=origin --push
```

## Création manuelle depuis GitHub

1. Créer un nouveau repository nommé `streamfolio`.
2. Ne pas ajouter de README, `.gitignore` ou licence depuis l'interface GitHub, car ils existent déjà localement.
3. Depuis la racine du projet :

```bash
git init
git add .
git commit -m "Initial commit: Streamfolio"
git branch -M main
git remote add origin git@github.com:<UTILISATEUR_GITHUB>/streamfolio.git
git push -u origin main
```

Remplacer `<UTILISATEUR_GITHUB>` par ton identifiant GitHub.
