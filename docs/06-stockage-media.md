# Mode média local

Phase 3.1 : l'application peut lire les médias depuis le classpath ou depuis un dossier externe.

Mode par défaut : `classpath`.

Mode local : `local-media`.

Dossier attendu en lancement Maven : `backend/data/media`.

Sous-dossiers attendus :

- `originals`
- `subtitles`
- `hls`

Commandes :

`bash scripts/prepare-local-media.sh`

`cd backend`

`mvn spring-boot:run -Dspring-boot.run.profiles=local-media`

Dans un second terminal :

`./scripts/smoke.sh`
