# Limites et administration média

Streamfolio reste une application de démonstration portfolio. Elle n'est pas une solution de production.

## Limites assumées

- Les fichiers uploadés sont stockés sur le système de fichiers local dans `streamfolio.media.root`.
- La base de données stocke uniquement les métadonnées : titres, vidéos, chemins, SHA-256, MIME, tailles et jobs.
- Les binaires vidéo, images et sous-titres ne sont pas stockés en base.
- PostgreSQL persiste les données métier, mais les sessions applicatives restent en mémoire dans le processus backend.
- L'administration est protégée par authentification et par le rôle `ADMIN`.
- FFmpeg est lancé localement par le backend.
- MinIO/S3 est préparé par configuration, mais l'adaptateur objet complet n'est pas encore branché.

## Workflow admin ajouté

Le panneau `#/admin` permet de lister, rechercher, filtrer, trier et paginer les vidéos. Il permet aussi d'uploader une nouvelle vidéo avec média, sous-titres VTT optionnels, poster, arrière-plan et métadonnées, puis d'éditer les métadonnées, lier une vidéo à un autre titre, ordonner une vidéo par saison/épisode, délier une vidéo en titre autonome et lancer un job de transcodage HLS.

La page `#/admin?view=jobs` suit les jobs de transcodage en cours, récents et passés sans rechargement manuel.

## Validation des fichiers

Les uploads sont refusés si le fichier est absent ou vide, si la taille dépasse les limites configurées, si l'extension ou le type MIME n'est pas autorisé, si le nom source contient un chemin, `..` ou un caractère de contrôle, ou si le nom final stocké n'est pas un SHA-256 attendu.

| Type | Extensions |
|---|---|
| Vidéo | `.mp4`, `.m4v`, `.mov`, `.qt`, `.wmv`, `.asf`, `.mkv`, `.webm`, `.avi`, `.divx`, `.flv`, `.f4v`, `.swf`, `.mts`, `.m2ts`, `.ts`, `.m2t`, `.mpeg`, `.mpg`, `.mpe`, `.m1v`, `.m2v`, `.m2p`, `.ps`, `.vob`, `.ogv`, `.ogg`, `.3gp`, `.3g2`, `.mxf`, `.dv`, `.rm`, `.rmvb`, `.mod`, `.tod`, `.dat` |
| Sous-titres | `.vtt` |
| Images | `.jpg`, `.jpeg`, `.png`, `.webp`, `.svg` |

## Stockage par SHA-256

Chaque fichier valide est lu, hashé en SHA-256, puis stocké sous la forme :

```text
backend/data/media/originals/{sha256}.{extension}
backend/data/media/subtitles/{sha256}.vtt
backend/data/media/posters/{sha256}.{extension}
backend/data/media/backdrops/{sha256}.{extension}
```

Si un fichier identique existe déjà, il n'est pas recopié. Une nouvelle vidéo peut réutiliser le même fichier physique via ses métadonnées.

## Règle film / série

- Un titre avec une seule vidéo est traité comme `MOVIE`.
- Un titre avec plusieurs vidéos liées est traité comme `SERIES`.
- Délier une vidéo crée un titre autonome de type `MOVIE`.

## Suite recommandée

Avant de considérer cette partie comme terminée : exécuter `bash scripts/validate.sh`, exécuter `npm run test:e2e`, vérifier la CI, renforcer la vérification technique des fichiers entrants, brancher MinIO/S3 pour sortir du stockage local et remplacer les sessions mémoire par un mécanisme partagé ou persistant.
