import { escapeHtml, progressBar } from './utils.js';

const VIDEO_ACCEPT = [
  'video/*',
  '.mp4', '.m4v', '.mov', '.wmv', '.mkv', '.webm', '.avi', '.flv', '.f4v', '.swf',
  '.mts', '.m2ts', '.ts', '.mpeg', '.mpg', '.mpe', '.m2v', '.vob', '.ogv', '.3gp', '.3g2', '.mxf'
].join(',');
const FORMAT_HINT = 'MP4, MOV, WMV, MKV, WebM/HTML5, AVI, FLV, F4V, SWF, AVCHD, MPEG-2, etc.';
const DEFAULT_YEAR = new Date().getFullYear();

export async function renderMediaAdmin(api) {
  const params = adminParams();
  const [videoPage, jobs, assets] = await Promise.all([
    api(`/api/admin/videos?${params.toString()}`).catch(() => ({ items: [], pagination: { number: 0, size: 20, totalElements: 0, totalPages: 0 } })),
    api('/api/admin/media/jobs').catch(() => []),
    api('/api/admin/media/assets').catch(() => []),
  ]);
  return `
    <section class='admin-page' aria-labelledby='admin-title'>
      <div class='admin-hero'>
        <p class='eyebrow'>Pipeline média</p>
        <h1 id='admin-title'>Administration vidéo</h1>
        <p class='lead'>Gère le catalogue, les fichiers locaux, les assets, les jobs FFmpeg et les regroupements film/série.</p>
        ${filtersView(params)}
      </div>
      <div class='admin-grid admin-grid-wide'>
        <section class='admin-panel admin-panel-main'>
          <h2>Vidéos</h2>
          ${videoPage.items?.length ? `<div class='admin-table'>${videoPage.items.map(videoView).join('')}</div>${paginationView(videoPage.pagination, params)}` : `<p class='muted'>Aucune vidéo ne correspond aux critères.</p>`}
        </section>
        <aside class='admin-panel'>
          <h2>Jobs récents</h2>
          ${jobs.length ? `<div class='admin-table'>${jobs.map(jobView).join('')}</div>` : `<p class='muted'>Aucun job pour le moment.</p>`}
          <h2 class='admin-subtitle'>Assets média</h2>
          ${assets.length ? `<div class='admin-table compact'>${assets.map(assetView).join('')}</div>` : `<p class='muted'>Aucun asset enregistré.</p>`}
        </aside>
      </div>
    </section>
  `;
}

export async function renderMediaAdminUpload() {
  return `
    <section class='admin-page admin-upload-page' aria-labelledby='admin-upload-title'>
      <div class='admin-hero'>
        <p class='eyebrow'>Nouvel asset</p>
        <h1 id='admin-upload-title'>Upload vidéo</h1>
        <p class='lead'>Crée une entrée de catalogue depuis un fichier vidéo, ses sous-titres, son affiche et son arrière-plan.</p>
        <div class='admin-actions'>
          <a class='btn ghost' href='#/admin'>Retour à l’administration</a>
        </div>
      </div>
      ${uploadView()}
    </section>
  `;
}

export async function handleMediaAdminSubmit(event, api, route) {
  const filter = event.target.closest('[data-admin-filter]');
  if (filter) {
    event.preventDefault();
    const params = new URLSearchParams(new FormData(filter));
    [...params.keys()].forEach((key) => { if (!params.get(key)) params.delete(key); });
    location.hash = `#/admin${params.toString() ? `?${params.toString()}` : ''}`;
    return true;
  }

  const upload = event.target.closest('[data-upload-form]');
  if (upload) {
    event.preventDefault();
    const body = new FormData(upload);
    ['durationSeconds', 'runtimeMinutes'].forEach((key) => {
      if (!String(body.get(key) || '').trim()) body.delete(key);
    });
    await api('/api/admin/videos', { method: 'POST', body });
    location.hash = '#/admin';
    route();
    return true;
  }

  const edit = event.target.closest('[data-admin-edit]');
  if (edit) {
    event.preventDefault();
    const data = formObject(edit);
    data.genres = splitGenres(data.genres);
    await api(`/api/admin/videos/${edit.dataset.videoId}`, { method: 'PUT', body: JSON.stringify(data) });
    route();
    return true;
  }

  const link = event.target.closest('[data-admin-link]');
  if (link) {
    event.preventDefault();
    const data = formObject(link);
    data.targetTitleId = Number(data.targetTitleId);
    await api(`/api/admin/videos/${link.dataset.videoId}/link`, { method: 'POST', body: JSON.stringify(data) });
    route();
    return true;
  }

  const order = event.target.closest('[data-admin-order]');
  if (order) {
    event.preventDefault();
    await api(`/api/admin/videos/${order.dataset.videoId}/order`, { method: 'PUT', body: JSON.stringify(formObject(order)) });
    route();
    return true;
  }

  const transcode = event.target.closest('[data-transcode-form]');
  if (transcode) {
    event.preventDefault();
    const data = new FormData(transcode);
    const videoId = Number(data.get('videoId')) || 1;
    const force = data.get('force') === 'on';
    await api(`/api/admin/media/videos/${videoId}/transcode`, { method: 'POST', body: JSON.stringify({ force }) });
    setTimeout(route, 350);
    return true;
  }

  return false;
}

export async function handleMediaAdminClick(event, route) {
  const tooltip = event.target.closest('[data-tooltip-toggle]');
  if (tooltip) {
    event.preventDefault();
    toggleTooltip(tooltip);
    return true;
  }

  const capture = event.target.closest('[data-capture-thumbnail]');
  if (capture) {
    event.preventDefault();
    await captureThumbnail(capture);
    return true;
  }

  const refresh = event.target.closest('[data-refresh-admin]');
  if (refresh) {
    event.preventDefault();
    route();
    return true;
  }
  const unlink = event.target.closest('[data-admin-unlink]');
  if (unlink) {
    event.preventDefault();
    await fetch(`/api/admin/videos/${unlink.dataset.videoId}/unlink`, { method: 'POST', credentials: 'same-origin' });
    route();
    return true;
  }
  return false;
}

export function handleMediaAdminInput(event) {
  const changedField = event.target.closest('[data-autofill-field]');
  if (changedField && event.isTrusted) changedField.dataset.autofilled = 'false';

  const mediaInput = event.target.closest('[data-media-file]');
  if (mediaInput) {
    handleMediaFile(mediaInput);
    return true;
  }

  const previewInput = event.target.closest('[data-image-preview]');
  if (previewInput) {
    updateImagePreview(previewInput);
    return true;
  }

  return false;
}

function adminParams() {
  const [, query = ''] = (location.hash || '').split('?');
  const params = new URLSearchParams(query);
  if (!params.has('page')) params.set('page', '0');
  if (!params.has('size')) params.set('size', '10');
  if (!params.has('sort')) params.set('sort', 'title,asc');
  return params;
}

function filtersView(params) {
  return `
    <form class='admin-form admin-filter-form' data-admin-filter>
      <label>Recherche <input name='query' type='search' value='${escapeHtml(params.get('query') || '')}' placeholder='titre, genre, fichier'></label>
      <label>Type <select name='type'><option value=''>Tous</option>${option('MOVIE', 'Films', params.get('type'))}${option('SERIES', 'Séries', params.get('type'))}</select></label>
      <label>Tri <select name='sort'>${['title,asc', 'title,desc', 'releaseYear,desc', 'videoTitle,asc', 'duration,desc', 'assetStatus,asc'].map((value) => option(value, value, params.get('sort'))).join('')}</select></label>
      <label>Taille <input name='size' type='number' min='1' max='100' value='${escapeHtml(params.get('size') || '10')}'></label>
      <input name='page' type='hidden' value='0'>
      <button class='btn primary' type='submit'>Filtrer</button>
      <a class='btn ghost' href='#/admin'>Réinitialiser</a>
      <a class='btn primary admin-upload-link' href='#/admin/upload'>Upload</a>
    </form>
  `;
}

function uploadView() {
  return `
    <section class='admin-panel admin-upload-panel'>
      <h2>Créer la vidéo</h2>
      <form class='admin-form admin-upload-form' data-upload-form enctype='multipart/form-data'>
        <div class='admin-field'>
          <label for='upload-media'>Fichier vidéo</label>
          <input id='upload-media' name='media' type='file' accept='${VIDEO_ACCEPT}' data-media-file required>
          <p class='field-help'>Formats acceptés : ${FORMAT_HINT}</p>
        </div>
        <div class='admin-field'><label for='upload-title'>Titre</label><input id='upload-title' name='title' data-autofill-field required></div>
        <div class='admin-field'><label for='upload-year'>Année</label><input id='upload-year' name='releaseYear' type='number' min='1888' value='${DEFAULT_YEAR}' data-autofill-field required></div>
        <div class='admin-field'><label for='upload-genres'>Genres</label><input id='upload-genres' name='genres' placeholder='Science, Drame' data-autofill-field required></div>
        <div class='admin-field'><label for='upload-synopsis'>Description</label><textarea id='upload-synopsis' name='synopsis' rows='5' data-autofill-field required></textarea></div>
        <div class='admin-field'>
          <div class='field-label-row'>
            <label for='upload-tagline'>Tagline</label>
            <span class='tooltip-wrap'>
              <button class='tooltip-button' type='button' aria-label='Aide sur la tagline' aria-expanded='false' data-tooltip-toggle>?</button>
              <span class='tooltip-popover' role='tooltip'>Phrase courte qui résume la promesse ou l’accroche marketing du titre. Elle s’affiche comme sous-titre éditorial, par exemple sous le titre dans le hero.</span>
            </span>
          </div>
          <input id='upload-tagline' name='tagline' data-autofill-field>
        </div>
        <div class='admin-field'><label for='upload-maturity'>Classification</label><input id='upload-maturity' name='maturityRating' placeholder='TV-PG, 12+, Tous publics' data-autofill-field></div>
        <div class='admin-field'><label for='upload-video-title'>Titre vidéo</label><input id='upload-video-title' name='videoTitle' data-autofill-field></div>
        <div class='admin-field'><label for='upload-label'>Label</label><input id='upload-label' name='label' placeholder='Film, Bande-annonce, S1:E1' data-autofill-field></div>
        <input name='durationSeconds' type='hidden' data-duration-seconds>
        <input name='runtimeMinutes' type='hidden' data-runtime-minutes>
        <div class='admin-field'>
          <span>Durée détectée</span>
          <p class='duration-status' data-duration-status>Choisis un fichier vidéo pour détecter la durée automatiquement.</p>
        </div>
        <div class='admin-field'>
          <label for='upload-subtitles'>Sous-titres VTT</label>
          <input id='upload-subtitles' name='subtitles' type='file' accept='.vtt,text/vtt' required>
        </div>
        <div class='admin-field'>
          <label for='upload-poster'>Affiche / miniature</label>
          <input id='upload-poster' name='poster' type='file' accept='image/*' data-image-preview data-preview-target='poster' required>
          <div class='admin-preview-frame'><img data-preview='poster' alt='Prévisualisation de l’affiche' hidden></div>
        </div>
        <div class='admin-field thumbnail-picker'>
          <label for='thumbnail-time'>Miniature depuis la vidéo</label>
          <div class='thumbnail-controls'>
            <input id='thumbnail-time' type='number' min='0' step='0.1' placeholder='Timestamp en secondes' data-thumbnail-time disabled>
            <button class='btn ghost' type='button' data-capture-thumbnail disabled>Utiliser ce timestamp</button>
          </div>
          <p class='field-help' data-thumbnail-status>Disponible après sélection d’un fichier vidéo lisible par le navigateur.</p>
        </div>
        <div class='admin-field'>
          <label for='upload-backdrop'>Arrière-plan</label>
          <input id='upload-backdrop' name='backdrop' type='file' accept='image/*' data-image-preview data-preview-target='backdrop' required>
          <div class='admin-preview-frame admin-preview-wide'><img data-preview='backdrop' alt='Prévisualisation de l’arrière-plan' hidden></div>
        </div>
        <div class='admin-actions'>
          <button class='btn primary' type='submit'>Créer</button>
          <a class='btn ghost' href='#/admin'>Annuler</a>
        </div>
      </form>
    </section>
  `;
}

function handleMediaFile(input) {
  const form = input.closest('[data-upload-form]');
  const file = input.files?.[0];
  setThumbnailControls(form, Boolean(file));
  if (!form || !file) {
    setDuration(form, null);
    return;
  }

  const derivedTitle = titleFromFilename(file.name);
  const derivedYear = yearFromFilename(file.name);
  setAutoValue(form.elements.title, derivedTitle);
  setAutoValue(form.elements.videoTitle, derivedTitle);
  setAutoValue(form.elements.label, 'Film');
  setAutoValue(form.elements.genres, 'Demo');
  setAutoValue(form.elements.synopsis, `Description à compléter pour ${derivedTitle}.`);
  setAutoValue(form.elements.maturityRating, 'TV-PG');
  if (derivedYear) setAutoValue(form.elements.releaseYear, String(derivedYear));

  detectDuration(form, file);
}

function detectDuration(form, file) {
  const status = form.querySelector('[data-duration-status]');
  setDuration(form, null);
  status.textContent = 'Lecture des métadonnées vidéo…';
  const video = document.createElement('video');
  const objectUrl = URL.createObjectURL(file);
  video.preload = 'metadata';
  video.muted = true;
  video.src = objectUrl;
  video.addEventListener('loadedmetadata', () => {
    const seconds = Math.max(1, Math.round(Number(video.duration) || 0));
    URL.revokeObjectURL(objectUrl);
    if (!Number.isFinite(seconds) || seconds < 1) {
      status.textContent = 'Durée non lisible par le navigateur ; le backend tentera de l’extraire.';
      return;
    }
    setDuration(form, seconds);
    status.textContent = `${formatSeconds(seconds)} détecté automatiquement.`;
  }, { once: true });
  video.addEventListener('error', () => {
    URL.revokeObjectURL(objectUrl);
    status.textContent = 'Durée non lisible par le navigateur ; le backend tentera de l’extraire.';
  }, { once: true });
}

function setDuration(form, seconds) {
  const duration = form?.querySelector('[data-duration-seconds]');
  const runtime = form?.querySelector('[data-runtime-minutes]');
  const thumbnailTime = form?.querySelector('[data-thumbnail-time]');
  if (!duration || !runtime) return;
  if (!seconds) {
    duration.value = '';
    runtime.value = '';
    thumbnailTime?.removeAttribute('max');
    return;
  }
  duration.value = String(seconds);
  runtime.value = String(Math.max(1, Math.ceil(seconds / 60)));
  if (thumbnailTime) thumbnailTime.max = String(seconds);
}

function setThumbnailControls(form, enabled) {
  form?.querySelectorAll('[data-thumbnail-time], [data-capture-thumbnail]').forEach((element) => {
    element.disabled = !enabled;
  });
  const status = form?.querySelector('[data-thumbnail-status]');
  if (status) status.textContent = enabled ? 'Indique un timestamp en secondes puis génère l’affiche depuis la vidéo.' : 'Disponible après sélection d’un fichier vidéo lisible par le navigateur.';
}

async function captureThumbnail(button) {
  const form = button.closest('[data-upload-form]');
  const mediaInput = form?.querySelector('[data-media-file]');
  const posterInput = form?.querySelector("input[name='poster']");
  const timeInput = form?.querySelector('[data-thumbnail-time]');
  const status = form?.querySelector('[data-thumbnail-status]');
  const file = mediaInput?.files?.[0];
  if (!form || !file || !posterInput || !timeInput) return;

  const requestedTime = Math.max(0, Number(timeInput.value || 0));
  status.textContent = 'Extraction de l’image…';
  button.disabled = true;
  try {
    const thumbnail = await frameFileFromVideo(file, requestedTime);
    const transfer = new DataTransfer();
    transfer.items.add(thumbnail);
    posterInput.files = transfer.files;
    updateImagePreview(posterInput);
    status.textContent = `Miniature générée à ${requestedTime.toFixed(1)} s.`;
  } catch (error) {
    status.textContent = error.message || 'Impossible d’extraire une image depuis ce fichier.';
  } finally {
    button.disabled = false;
  }
}

async function frameFileFromVideo(file, requestedTime) {
  const objectUrl = URL.createObjectURL(file);
  const video = document.createElement('video');
  video.preload = 'metadata';
  video.muted = true;
  video.playsInline = true;
  video.src = objectUrl;
  try {
    await waitForMedia(video, 'loadedmetadata');
    const duration = Number.isFinite(video.duration) ? video.duration : requestedTime;
    video.currentTime = Math.min(Math.max(0, requestedTime), Math.max(0, duration - 0.05));
    await waitForMedia(video, 'seeked');
    const width = video.videoWidth || 1280;
    const height = video.videoHeight || 720;
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    canvas.getContext('2d').drawImage(video, 0, 0, width, height);
    const blob = await new Promise((resolve, reject) => canvas.toBlob((value) => value ? resolve(value) : reject(new Error('Capture image impossible.')), 'image/jpeg', 0.92));
    return new File([blob], `${filenameStem(file.name)}-thumbnail.jpg`, { type: 'image/jpeg' });
  } catch (error) {
    throw new Error('Ce format ne peut pas être prévisualisé par le navigateur pour l’extraction de miniature.');
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

function waitForMedia(element, eventName) {
  return new Promise((resolve, reject) => {
    const cleanup = () => {
      element.removeEventListener(eventName, onEvent);
      element.removeEventListener('error', onError);
    };
    const onEvent = () => { cleanup(); resolve(); };
    const onError = () => { cleanup(); reject(new Error('Erreur de lecture média.')); };
    element.addEventListener(eventName, onEvent, { once: true });
    element.addEventListener('error', onError, { once: true });
  });
}

function updateImagePreview(input) {
  const form = input.closest('[data-upload-form]');
  const previewName = input.dataset.previewTarget;
  const image = form?.querySelector(`[data-preview='${previewName}']`);
  const file = input.files?.[0];
  if (!image || !file) return;
  if (image.dataset.objectUrl) URL.revokeObjectURL(image.dataset.objectUrl);
  const objectUrl = URL.createObjectURL(file);
  image.dataset.objectUrl = objectUrl;
  image.src = objectUrl;
  image.hidden = false;
}

function setAutoValue(field, value) {
  if (!field || !value) return;
  if (!field.value || field.dataset.autofilled === 'true') {
    field.value = value;
    field.dataset.autofilled = 'true';
  }
}

function titleFromFilename(filename) {
  return filenameStem(filename)
    .replace(/\b(19|20)\d{2}\b/g, ' ')
    .replace(/\b(2160p|1080p|720p|480p|web[-_. ]?dl|bluray|brrip|hdrip|x264|x265|h264|h265|hevc|aac|dts)\b/gi, ' ')
    .replace(/[._\-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim() || 'Nouvelle vidéo';
}

function filenameStem(filename) {
  return String(filename || 'video').replace(/\.[^.]+$/, '');
}

function yearFromFilename(filename) {
  const match = String(filename || '').match(/(?:^|\D)((?:19|20)\d{2})(?:\D|$)/);
  return match ? Number(match[1]) : null;
}

function formatSeconds(totalSeconds) {
  const total = Math.max(0, Number(totalSeconds) || 0);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const seconds = total % 60;
  return [hours ? `${hours} h` : '', minutes ? `${minutes} min` : '', `${seconds} s`].filter(Boolean).join(' ');
}

function toggleTooltip(button) {
  const wrapper = button.closest('.tooltip-wrap');
  const nextOpen = !wrapper.classList.contains('is-open');
  document.querySelectorAll('.tooltip-wrap.is-open').forEach((item) => {
    item.classList.remove('is-open');
    item.querySelector('[data-tooltip-toggle]')?.setAttribute('aria-expanded', 'false');
  });
  wrapper.classList.toggle('is-open', nextOpen);
  button.setAttribute('aria-expanded', String(nextOpen));
}

function videoView(video) {
  return `
    <article class='admin-row admin-video-row'>
      <div>
        <strong>#${video.videoId} · ${escapeHtml(video.title)}</strong>
        <p>${escapeHtml(video.videoTitle)} · ${escapeHtml(video.type)} · ${video.releaseYear} · ${escapeHtml((video.genres || []).join(', '))}</p>
        <p>${escapeHtml(video.assetFilename)} · ${video.sizeBytes || 0} octets · SHA ${escapeHtml(video.contentSha256 || 'non enregistré')}</p>
        <div class='admin-actions'>
          <form data-transcode-form><input name='videoId' type='hidden' value='${video.videoId}'><label class='checkline'><input name='force' type='checkbox'> force</label><button class='btn ghost' type='submit'>Transcoder</button></form>
          <button class='btn ghost' type='button' data-admin-unlink data-video-id='${video.videoId}'>Délier</button>
        </div>
        <details>
          <summary>Éditer / lier / ordonner</summary>
          <form class='admin-form admin-inline-form' data-admin-edit data-video-id='${video.videoId}'>
            <label>Titre <input name='title' value='${escapeHtml(video.title)}'></label>
            <label>Année <input name='releaseYear' type='number' value='${video.releaseYear}'></label>
            <label>Genres <input name='genres' value='${escapeHtml((video.genres || []).join(', '))}'></label>
            <label>Description <textarea name='synopsis'>${escapeHtml(video.synopsis || '')}</textarea></label>
            <label>Titre vidéo <input name='videoTitle' value='${escapeHtml(video.videoTitle)}'></label>
            <label>Label <input name='label' value='${escapeHtml(video.label)}'></label>
            <label>Saison <input name='seasonNumber' type='number' min='0' value='${video.seasonNumber}'></label>
            <label>Épisode <input name='episodeNumber' type='number' min='0' value='${video.episodeNumber}'></label>
            <label>Durée s <input name='durationSeconds' type='number' min='1' value='${video.durationSeconds}'></label>
            <button class='btn primary' type='submit'>Enregistrer</button>
          </form>
          <form class='admin-form admin-inline-form' data-admin-link data-video-id='${video.videoId}'>
            <label>ID titre cible <input name='targetTitleId' type='number' min='1' required></label>
            <label>Saison <input name='seasonNumber' type='number' min='0' value='1'></label>
            <label>Épisode <input name='episodeNumber' type='number' min='0' value='1'></label>
            <button class='btn ghost' type='submit'>Lier</button>
          </form>
          <form class='admin-form admin-inline-form' data-admin-order data-video-id='${video.videoId}'>
            <label>Saison <input name='seasonNumber' type='number' min='0' value='${video.seasonNumber}'></label>
            <label>Épisode <input name='episodeNumber' type='number' min='0' value='${video.episodeNumber}'></label>
            <button class='btn ghost' type='submit'>Réordonner</button>
          </form>
        </details>
      </div>
      <span class='status-pill status-${String(video.assetStatus || 'missing').toLowerCase()}'>${escapeHtml(video.assetStatus || 'NO_ASSET')}</span>
    </article>
  `;
}

function paginationView(pagination, params) {
  const current = Number(pagination?.number || 0);
  const total = Number(pagination?.totalPages || 0);
  return `<div class='admin-pagination'><a class='btn ghost${current <= 0 ? ' disabled' : ''}' href='${pageHref(params, Math.max(0, current - 1))}'>Précédent</a><span>${current + 1} / ${Math.max(1, total)}</span><a class='btn ghost${current >= total - 1 ? ' disabled' : ''}' href='${pageHref(params, current + 1)}'>Suivant</a></div>`;
}

function pageHref(params, page) {
  const next = new URLSearchParams(params);
  next.set('page', String(page));
  return `#/admin?${next.toString()}`;
}

function jobView(job) {
  return `<article class='admin-row'><div><strong>#${job.id} · ${escapeHtml(job.title)} — ${escapeHtml(job.videoTitle)}</strong><p>${escapeHtml(job.message || '')}</p>${progressBar(job.progressPercent || 0, 'Progression ' + (job.progressPercent || 0) + '%')}</div><span class='status-pill status-${String(job.status || '').toLowerCase()}'>${escapeHtml(job.status)}</span></article>`;
}

function assetView(asset) {
  return `<article class='admin-row'><div><strong>${escapeHtml(asset.title)} — ${escapeHtml(asset.videoTitle)}</strong><p>${escapeHtml(asset.originalFilename)} · ${escapeHtml(asset.thumbnailManifestPath || 'pas de thumbnails')}</p></div><span class='status-pill status-${String(asset.status || '').toLowerCase()}'>${escapeHtml(asset.status)}</span></article>`;
}

function formObject(form) {
  return Object.fromEntries([...new FormData(form).entries()].map(([key, value]) => [key, numericValue(value)]));
}

function numericValue(value) {
  const text = String(value ?? '').trim();
  return /^\d+$/.test(text) ? Number(text) : text;
}

function splitGenres(value) {
  return String(value || '').split(',').map((item) => item.trim()).filter(Boolean);
}

function option(value, label, selected) {
  return `<option value='${escapeHtml(value)}'${value === selected ? ' selected' : ''}>${escapeHtml(label)}</option>`;
}
