# Vérification

## Vérifications réalisées dans l'environnement de génération

- Inspection du ZIP fourni.
- Lecture des fichiers frontend, backend, scripts et documentation.
- Réécriture contrôlée des fichiers UI statiques.
- Ajout déterministe de l'endpoint `/api/genres`.
- Ajout de données de démonstration idempotentes.
- Vérification syntaxique JavaScript avec `node --check backend/src/main/resources/static/app.js`.
- Construction d'un ZIP propre sans `target/` ni base H2 locale.

## Limite de vérification

Maven n'était pas installé dans l'environnement d'exécution utilisé pour générer ce livrable. Les commandes `mvn test` et `mvn spring-boot:run` n'ont donc pas pu être exécutées ici.

## Vérification locale recommandée

```bash
cd backend
mvn test
mvn spring-boot:run
```

Dans un autre terminal :

```bash
./scripts/smoke.sh
```

Ouvrir ensuite :

```text
http://localhost:8080
```

Compte :

```text
alexis@example.dev / demo1234
```

## Vérification V2 — UI épurée

Changements appliqués :

- cartes réduites à une miniature 16:9 avec titre superposé sur fond semi-transparent ;
- aperçu au survol avec description, type, genres, année, classification, durée ou nombre de saisons/épisodes ;
- bouton triangulaire semi-transparent pour ouvrir la fiche détail ;
- suppression du libellé `Streamfolio` dans la barre supérieure : le logo seul renvoie au catalogue ;
- bouton de déconnexion remplacé par une icône de profil ;
- flèches de carrousel affichées uniquement en cas de débordement horizontal ;
- navigation circulaire des carrousels : première carte → dernière, dernière carte → première ;
- augmentation de l'opacité des boutons textuels semi-transparents ;
- cache PWA incrémenté en `streamfolio-shell-v3`.

Contrôles exécutés dans l'environnement de génération :

```bash
node --check backend/src/main/resources/static/app.js
bash -n scripts/run.sh scripts/smoke.sh
```

Limite : Maven n'est pas disponible dans l'environnement de génération, donc les tests Maven n'ont pas pu être exécutés localement.

## Vérification V3 — scroll, aperçus et miniatures

Changements appliqués :

- les pop-ups ne sont plus rendues dans chaque carte ; elles sont générées dynamiquement en modale fixe centrée ;
- la modale s'ouvre uniquement au clic sur la miniature, pas au survol ;
- le bouton lecture reste indépendant du clic miniature ;
- animation d'ouverture : fondu + agrandissement progressif ;
- réduction des espacements entre sections ;
- suppression des grands `padding-bottom` réservés aux anciennes pop-ups de cartes ;
- rails horizontaux sans scrollbar visible ;
- flèches de carrousel conservées uniquement en cas de débordement ;
- miniatures SVG régénérées sans éléments `<text>` ni `<tspan>` visibles ;
- cache PWA incrémenté en `streamfolio-shell-v4`.

Contrôles à exécuter ou exécutés selon disponibilité locale :

```bash
node --check backend/src/main/resources/static/app.js
bash -n scripts/run.sh scripts/smoke.sh
grep -R "<text\|<tspan" -n backend/src/main/resources/static/assets/posters
```

La commande `grep` ne doit retourner aucun résultat.


## V5

Contrôles ajoutés :

```bash
node --check backend/src/main/resources/static/app.js
bash -n scripts/run.sh scripts/smoke.sh scripts/reset-dev-state.sh
python3 -m py_compile scripts/regenerate-posters.py
grep -R "<text\|<tspan" -n backend/src/main/resources/static/assets/posters backend/src/main/resources/static/assets/posters-clean
zip -T streamfolio_refonte_ui_netflix_like_v5.zip
```

Résultat attendu pour `grep` : aucune ligne.

Points couverts :

- scrollbars visibles stylisées ;
- carrousels sans scrollbar interne visible ;
- flèches visibles uniquement au survol/focus du carrousel ;
- miniatures sans texte incrusté dans un nouveau dossier `posters-clean` ;
- remplacement des anciennes miniatures legacy ;
- seed Java en mode refresh/upsert pour corriger une base existante.

## V6

Contrôles ajoutés :

```bash
node --check backend/src/main/resources/static/app.js
bash -n scripts/run.sh scripts/smoke.sh scripts/reset-dev-state.sh
python3 -m py_compile scripts/regenerate-posters.py
grep -R "Interface sombre\|Sélection éditoriale\|Découverte rapide\|Explorer par ambiance\|Entrées directes vers les genres" -n backend/src/main/resources/static/app.js
zip -T streamfolio_refonte_ui_netflix_like_v6.zip
```

Points couverts :

- page détail glassmorphism ;
- liste d’épisodes uniquement pour les séries ;
- bouton détail `Ma liste` compact ;
- suppression des textes hero/discovery demandés ;
- flèches de carrousel au-dessus des cartes lorsqu’une carte d’extrémité est survolée.

## V7 — vues catalogue unifiées

- `Accueil`, `Films`, `Séries` et `Ma liste` utilisent le même rendu par hero + section `Explorer` + rails horizontaux.
- Les anciennes vues grille avec `Catalogue`, `Tous les titres` et compteur de résultats ne sont plus affichées.
- L'entrée visible `Catalogue` a été retirée de la navigation principale et mobile.
- Les données `/api/sections` et `/api/genres` sont mises en cache côté client pendant la session pour éviter un écran de rechargement lors du passage entre les vues de navigation.
- Le cache PWA est incrémenté en `streamfolio-shell-v8`.
