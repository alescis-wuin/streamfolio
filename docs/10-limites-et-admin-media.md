# Limites et administration media

Streamfolio reste une application de demonstration portfolio. Elle n'est pas une solution de production.

## Limites assumees

- Les fichiers uploades sont stockes sur le systeme de fichiers local dans `streamfolio.media.root`.
- La base de donnees stocke uniquement les metadonnees: titres, videos, chemins, SHA-256, MIME, tailles et jobs.
- Les binaires video, images et sous-titres ne sont pas stockes en base.
- Les sessions applicatives sont en memoire et sont perdues au redemarrage.
- L'administration est protegee par authentification, mais il n'existe pas encore de role `ADMIN` separe.
- FFmpeg est lance localement par le backend.
- MinIO/S3 est prepare par configuration, mais l'adaptateur objet complet n'est pas branche.

## Workflow admin ajoute

Le panneau `#/admin` permet de lister, rechercher, filtrer, trier et paginer les videos. Il permet aussi d'uploader une nouvelle video avec media, sous-titres VTT, poster, arriere-plan et metadonnees, puis d'editer les metadonnees, lier une video a un autre titre, ordonner une video par saison/episode, delier une video en titre autonome et lancer un job de transcodage HLS.

## Validation des fichiers

Les uploads sont refuses si le fichier est absent ou vide, si la taille depasse les limites configurees, si l'extension ou le type MIME n'est pas autorise, si le nom source contient un chemin, `..` ou un caractere de controle, ou si le nom final stocke n'est pas un SHA-256 attendu.

| Type | Extensions |
|---|---|
| Video | `.mp4`, `.m4v`, `.mov`, `.mkv`, `.webm` |
| Sous-titres | `.vtt` |
| Images | `.jpg`, `.jpeg`, `.png`, `.webp`, `.svg` |

## Stockage par SHA-256

Chaque fichier valide est lu, hashe en SHA-256, puis stocke sous la forme :

```text
backend/data/media/originals/{sha256}.mp4
backend/data/media/subtitles/{sha256}.vtt
backend/data/media/posters/{sha256}.jpg
backend/data/media/backdrops/{sha256}.jpg
```

Si un fichier identique existe deja, il n'est pas recopie. Une nouvelle video peut reutiliser le meme fichier physique via ses metadonnees.

## Regle film / serie

- Un titre avec une seule video est traite comme `MOVIE`.
- Un titre avec plusieurs videos liees est traite comme `SERIES`.
- Delier une video cree un titre autonome de type `MOVIE`.

## Suite recommandee

Avant de considerer cette partie comme terminee : executer `bash scripts/validate.sh`, executer `npm run test:e2e`, verifier la CI, ajouter un role `ADMIN` distinct, enrichir la verification technique des fichiers entrants, brancher MinIO/S3 pour sortir du stockage local et remplacer les sessions memoire par un mecanisme partage ou persistant.
