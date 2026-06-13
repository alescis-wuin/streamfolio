# Mode média local et HLS

Phase 3.1 : l'application peut lire les médias depuis le classpath ou depuis un dossier externe.

Phase 3.2.A : l'infrastructure FFmpeg peut générer une sortie HLS locale.

Phase 3.2.B : l'API expose les playlists et segments HLS derrière authentification.

Phase 3.2.C : le lecteur frontend choisit HLS quand disponible et retombe sur MP4 si nécessaire.

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

Endpoints HLS protégés :

- `/api/videos/1/hls/master.m3u8`
- `/api/videos/1/hls/segment_000.ts`

Le DTO playback expose toujours `streamUrl` pour le fallback MP4. Quand HLS existe, il ajoute `hlsUrl` et `streamingMode=HLS_AVAILABLE`.

Le lecteur frontend utilise hls.js quand il est disponible. Si hls.js ne peut pas être chargé ou si HLS n'est pas supporté par le navigateur, le lecteur utilise automatiquement `streamUrl` en MP4 progressif.

Lancer en mode local :

`cd backend`

`mvn spring-boot:run -Dspring-boot.run.profiles=local-media`

Dans un second terminal :

`./scripts/smoke.sh`

Après une mise à jour frontend, effectuer un rafraîchissement complet du navigateur si l'ancien Service Worker sert encore l'ancien `app.js`.
