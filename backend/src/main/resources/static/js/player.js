import { escapeHtml } from './utils.js';

const HLS_SCRIPT_URL = 'https://cdn.jsdelivr.net/npm/hls.js@1';
let hlsLibraryPromise = null;

export function playerModeView(title, detail) {
  return `<span class='mode-dot' aria-hidden='true'></span><div><strong>${escapeHtml(title)}</strong><span>${escapeHtml(detail)}</span></div>`;
}

export function initialPlaybackModeText(playback) {
  if (playback.streamingMode === 'HLS_AVAILABLE') return 'Flux HLS détecté. Initialisation du lecteur adaptatif.';
  if (playback.streamingMode === 'HLS_MISSING') return 'HLS absent pour cette vidéo. Lecture MP4 progressive.';
  return 'Lecture MP4 progressive.';
}

export function setupPlayer(playback, options) {
  const video = document.querySelector('#video-player');
  if (!video) return;
  const status = document.querySelector('#player-mode');
  const startPosition = playback.progress?.positionSeconds || 0;
  let lastSync = 0;
  let disposed = false;
  let hlsInstance = null;

  configureVideoSource(video, playback, status).then((instance) => {
    if (disposed) {
      instance?.destroy?.();
      return;
    }
    hlsInstance = instance;
  });

  loadTimeline(playback.thumbnailManifestUrl);

  video.addEventListener('loadedmetadata', () => {
    if (startPosition > 0 && startPosition < video.duration - 3) video.currentTime = startPosition;
  }, { once: true });

  const syncProgress = async (force = false) => {
    if ((disposed && !force) || !Number.isFinite(video.duration) || video.duration <= 0) return;
    const now = Date.now();
    if (!force && now - lastSync < 1800 && !video.paused && !video.ended) return;
    lastSync = now;
    try {
      await options.api(`/api/videos/${playback.videoId}/progress`, {
        method: 'PUT',
        keepalive: force,
        body: JSON.stringify({ positionSeconds: Math.floor(video.currentTime), durationSeconds: Math.floor(video.duration) }),
      });
    } catch {
      // Non bloquant.
    }
  };

  const onKey = (event) => {
    if (event.target && ['INPUT', 'TEXTAREA', 'SELECT'].includes(event.target.tagName)) return;
    if (!location.hash.startsWith('#/watch/')) return;
    if (event.key === 'k' || event.key === ' ') { event.preventDefault(); video.paused ? video.play() : video.pause(); }
    if (event.key === 'ArrowRight') { event.preventDefault(); video.currentTime = Math.min(video.duration || Infinity, video.currentTime + 10); }
    if (event.key === 'ArrowLeft') { event.preventDefault(); video.currentTime = Math.max(0, video.currentTime - 10); }
    if (event.key.toLowerCase() === 'm') { event.preventDefault(); video.muted = !video.muted; }
    if (event.key.toLowerCase() === 'f') { event.preventDefault(); video.requestFullscreen?.(); }
  };

  video.addEventListener('timeupdate', () => syncProgress(false));
  video.addEventListener('pause', () => syncProgress(true));
  video.addEventListener('ended', () => syncProgress(true));
  document.addEventListener('keydown', onKey);
  window.addEventListener('beforeunload', () => syncProgress(true));

  options.setCleanup(() => {
    syncProgress(true);
    disposed = true;
    hlsInstance?.destroy?.();
    document.removeEventListener('keydown', onKey);
  });
}

async function configureVideoSource(video, playback, status) {
  if (playback.streamingMode === 'HLS_AVAILABLE' && playback.hlsUrl) {
    const hls = await setupHlsPlayback(video, playback, status);
    if (hls) return hls;
  }
  applyMp4Fallback(video, playback, status, fallbackReason(playback));
  return null;
}

async function setupHlsPlayback(video, playback, status) {
  setPlayerMode(status, 'HLS disponible', 'Chargement du lecteur adaptatif…', 'hls');
  try {
    const Hls = await loadHlsLibrary();
    if (Hls?.isSupported?.()) {
      const hls = new Hls({ xhrSetup: (xhr) => { xhr.withCredentials = true; } });
      hls.loadSource(playback.hlsUrl);
      hls.attachMedia(video);
      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (data?.fatal) {
          hls.destroy();
          applyMp4Fallback(video, playback, status, 'Erreur HLS détectée. Repli automatique sur MP4.');
        }
      });
      setPlayerMode(status, 'HLS adaptatif', 'Lecture via hls.js. Fallback MP4 disponible.', 'hls');
      return hls;
    }
  } catch {
    // fallback below
  }

  if (video.canPlayType('application/vnd.apple.mpegurl')) {
    video.src = playback.hlsUrl;
    setPlayerMode(status, 'HLS natif', 'Lecture HLS native du navigateur. Fallback MP4 disponible.', 'hls');
    return null;
  }

  applyMp4Fallback(video, playback, status, 'HLS non supporté ici. Lecture MP4 progressive.');
  return null;
}

function applyMp4Fallback(video, playback, status, reason) {
  video.src = playback.streamUrl;
  setPlayerMode(status, 'MP4 fallback', reason, 'mp4');
}

function loadHlsLibrary() {
  if (window.Hls) return Promise.resolve(window.Hls);
  if (hlsLibraryPromise) return hlsLibraryPromise;
  hlsLibraryPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = HLS_SCRIPT_URL;
    script.async = true;
    script.dataset.hlsjs = 'true';
    script.onload = () => resolve(window.Hls);
    script.onerror = () => reject(new Error('Impossible de charger hls.js.'));
    document.head.appendChild(script);
  });
  return hlsLibraryPromise;
}

function setPlayerMode(target, title, detail, variant) {
  if (!target) return;
  target.classList.toggle('is-hls', variant === 'hls');
  target.classList.toggle('is-mp4', variant === 'mp4');
  target.innerHTML = playerModeView(title, detail);
}

function fallbackReason(playback) {
  if (playback.streamingMode === 'HLS_MISSING') return 'Playlist HLS absente. Lecture MP4 progressive.';
  if (playback.streamingMode === 'MP4_ONLY') return 'Mode démo classpath. Lecture MP4 progressive.';
  return 'Lecture MP4 progressive.';
}

async function loadTimeline(manifestUrl) {
  const target = document.querySelector('#timeline-thumbnails');
  if (!target || !manifestUrl) return;
  try {
    const response = await fetch(manifestUrl, { credentials: 'same-origin' });
    if (!response.ok) return;
    const manifest = await response.json();
    target.innerHTML = `<div class='timeline-strip'>${(manifest.items || []).map((item) => `<button type='button' data-seek='${Number(item.timeSeconds) || 0}' title='${Number(item.timeSeconds) || 0}s'><img src='${escapeHtml(item.url)}' alt='Miniature ${Number(item.timeSeconds) || 0}s'></button>`).join('')}</div>`;
    target.querySelectorAll('[data-seek]').forEach((button) => {
      button.addEventListener('click', () => {
        const video = document.querySelector('#video-player');
        if (video) video.currentTime = Number(button.dataset.seek) || 0;
      });
    });
  } catch {
    target.innerHTML = '';
  }
}
