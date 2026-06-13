# Frontend

La V1.1 utilise une PWA sans dépendance frontend externe afin de rester immédiatement testable depuis Spring Boot.

Les fichiers servis sont dans :

```text
backend/src/main/resources/static/
```

## Choix conservé

Le projet pourrait être migré vers React/Vite, Vue, Svelte, Kotlin Compose Multiplatform, Tauri ou Capacitor. Pour cette itération portfolio, le choix retenu est volontairement plus sobre : HTML/CSS/JavaScript natif, car il permet de démontrer l'UX, les endpoints Java et le streaming sans ajouter de build frontend.

## Points UI refondus

- hero immersif ;
- rails horizontaux ;
- cartes 16:9 ;
- navigation supérieure et navigation mobile ;
- catalogue filtrable ;
- page “Ma liste” ;
- fiche détail plus visuelle ;
- lecteur accessible avec raccourcis clavier ;
- réduction du bruit visuel et absence d'autoplay.


## Notes V5

Les miniatures utilisées par l’UI se trouvent maintenant dans `backend/src/main/resources/static/assets/posters-clean/`.
Les titres visibles sur les cartes sont rendus par l’interface dans une bande semi-transparente, et non par les fichiers SVG.
