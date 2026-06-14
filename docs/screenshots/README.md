# Captures d'écran

Objectif : fournir des visuels courts, propres et directement exploitables dans le README et le portfolio.

## Préparation

```bash
cd backend
mvn spring-boot:run
```

Ouvrir ensuite `http://localhost:8080` et se connecter avec :

```text
alexis@example.dev / demo1234
```

Avant chaque capture :

- utiliser Chrome ou Firefox ;
- désactiver les extensions visibles ;
- vider les notifications éventuelles ;
- laisser l'interface finir de charger ;
- éviter de capturer la barre d'adresse si l'outil le permet.

## Captures à produire

| Fichier | Format | Où la prendre | Cadrage attendu |
|---|---:|---|---|
| `home.png` | 1440 × 900 | Accueil après connexion : `#/` | Hero visible, navigation supérieure, au moins deux rails de contenu |
| `detail.png` | 1440 × 900 | Fiche `Aurora Drift` ou `Botanical Cities` | Poster, synopsis, métadonnées, boutons `Lire` et `Ma liste` visibles |
| `player.png` | 1440 × 900 | Lecteur lancé depuis une fiche | Vidéo en cours, barre de contrôle visible, zone raccourcis clavier visible si possible |
| `mobile.png` | 390 × 844 | Accueil ou fiche détail avec émulation mobile | Navigation mobile, contenu lisible, aucun débordement horizontal |
| `demo.gif` | 1440 × 900 | Parcours complet court | Login → recherche → fiche détail → lecture → ajout/retrait Ma liste |

Tous les fichiers doivent être placés ici :

```text
docs/screenshots/
```

## Parcours conseillé pour le GIF

Durée cible : 15 à 25 secondes. Taille cible : moins de 10 Mo.

Scénario recommandé :

1. écran de connexion avec identifiants déjà préremplis ;
2. clic sur `Entrer` ;
3. arrivée sur l'accueil ;
4. recherche `botanical` ;
5. ouverture de `Botanical Cities` ;
6. clic sur `Ma liste` ;
7. clic sur `Lire` ;
8. lecture pendant 2 à 3 secondes ;
9. retour à la fiche ou arrêt sur le lecteur.

Réglages recommandés :

- 1440 × 900 ;
- 12 à 15 images/s ;
- curseur visible ;
- pas d'audio ;
- compression GIF ou WebM converti en GIF ;
- nom final : `docs/screenshots/demo.gif`.

## Insertion dans le README

Une fois les fichiers ajoutés, remplacer le bloc d'exemple du README par :

```md
![Accueil Streamfolio](docs/screenshots/home.png)
![Fiche détail Streamfolio](docs/screenshots/detail.png)
![Lecteur Streamfolio](docs/screenshots/player.png)

![Démo Streamfolio](docs/screenshots/demo.gif)
```
