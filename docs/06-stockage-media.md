# Mode média local et HLS

Phase 3.1 : l'application peut lire les médias depuis le classpath ou depuis un dossier externe.

Phase 3.2.A : l'infrastructure FFmpeg peut générer une sortie HLS locale.

Mode par défaut : `classpath`.

Mode local : `local-media`.

Dossier attendu en lancement Maven : `backend/data/media`.

Sous-dossiers attendus :

- `originals`
- `subtitles`
- `hls`

Préparer les médias locaux :

`bash scripts/prepare-local-media.sh`

Transcoder une vidéo de démonstration vers HLS :

`bash scripts/transcode-demo-hls.sh 1 aurora-drift.mp4`

Sortie attendue :

`backend/data/media/hls/1/master.m3u8`

Lancer en mode local :

`cd backend`

`mvn spring-boot:run -Dspring-boot.run.profiles=local-media`

Dans un second terminal :

`./scripts/smoke.sh`

Note : la phase 3.2.A ne branche pas encore le lecteur frontend sur HLS. Les endpoints HLS protégés et le fallback lecteur arriveront en phase 3.2.B et 3.2.C.
