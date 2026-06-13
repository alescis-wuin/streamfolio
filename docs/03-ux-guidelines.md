# Guidelines UI/UX

## Direction visuelle

- Fond noir profond, panneaux translucides et très peu de bordures.
- Accent rouge utilisé uniquement pour les actions principales et les états actifs.
- Typographie système, forte hiérarchie, grands titres, textes secondaires courts.
- Cartes 16:9 pour se rapprocher des usages de streaming sur écrans larges.
- Affiches abstraites originales, sans contenu protégé.
- Animations brèves, désactivables via `prefers-reduced-motion`.

## Navigation

- Barre supérieure fixe : Accueil, Séries, Films, Ma liste, Catalogue.
- Navigation mobile inférieure pour les vues principales.
- Recherche globale accessible dès toutes les pages connectées.
- Filtres visibles dans le catalogue : type et genre.
- Fiche détail séparée pour garder l'accueil sobre.
- Lecteur dédié, sans bruit visuel autour de la vidéo.

## Cartes

- Image 16:9.
- Bouton lecture circulaire au centre de l'image au survol ou au focus.
- Progression affichée directement sur l'image si lecture commencée.
- Description tronquée sur deux lignes.
- Bouton détail sous forme d'orbe discret.

## Accessibilité

- Chaque action est un bouton ou un lien réel.
- Focus visible sur tous les éléments interactifs.
- Contrôles natifs du lecteur.
- Sous-titres disponibles.
- Pas d'autoplay.
- Contraste fort.
- Navigation clavier du lecteur : `k`, espace, flèches, `m`, `f`.
- Libellés `aria-label` sur les boutons de lecture et détails.

## Expérience portfolio

À mettre en avant dans une démo :

- backend Java/Spring structuré ;
- streaming Range ;
- progression persistable ;
- PWA responsive ;
- catalogue enrichi ;
- UI inspirée des codes streaming modernes ;
- absence de dépendance frontend lourde en V1 ;
- roadmap technique claire.
