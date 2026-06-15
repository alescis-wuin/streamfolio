import { escapeHtml, progressBar } from './utils.js';

const VIDEO_ACCEPT = [
  'video/*',
  '.mp4', '.m4v', '.mov', '.qt', '.wmv', '.asf', '.mkv', '.webm', '.avi', '.divx',
  '.flv', '.f4v', '.swf', '.mts', '.m2ts', '.ts', '.m2t', '.mpeg', '.mpg', '.mpe',
  '.m1v', '.m2v', '.m2p', '.ps', '.vob', '.ogv', '.ogg', '.3gp', '.3g2', '.mxf',
  '.dv', '.rm', '.rmvb', '.mod', '.tod', '.dat'
].join(',');
const FORMAT_HINT = 'MP4, MOV, WMV, MKV, WebM/HTML5, AVI, FLV, F4V, SWF, AVCHD, MPEG-2, MPEG-TS, OGV, 3GP, MXF, DV, RealMedia, MOD/TOD, etc.';
const DEFAULT_YEAR = new Date().getFullYear();
const JOBS_VIEW = 'jobs';
const JOBS_POLL_INTERVAL_MS = 2000;
const RECENT_JOB_WINDOW_MS = 24 * 60 * 60 * 1000;
const RECENT_JOB_FALLBACK_LIMIT = 5;

let jobsApi = null;
let jobsPollTimer = null;
let jobsRefreshInFlight = false;

export async function renderMediaAdmin(api) {
  const params = adminParams();
  if (params.get('view') === JOBS_VIEW) {
    return renderMediaAdminJobs(api);
  }

  stopJobsPolling();
  const [videoPage, assets] = await Promise.all([
    api(`/api/admin/videos?${params.toString()}`).catch(() => ({ items: [], pagination: { number: 0, size: 20, totalElements: 0, totalPages: 0 } })),
    api('/api/admin/media/assets').catch(() => []),
  ]);
  return `
    <section class='admin-page' aria-labelledby='admin-title'>
      <div class='admin-hero'>
        <p class='eyebrow'>Pipeline média</p>
        <h1 id='admin-title'>Administration vidéo</h1>
        <p class='lead'>Gère le catalogue, les fichiers locaux, les assets et les regroupements film/série.</p>
        ${filtersView(params)}
      </div>
      <div class='admin-grid admin-grid-wide'>
        <section class='admin-panel admin-panel-main'>
          <h2>Vidéos</h2>
          ${videoPage.items?.length ? `<div class='admin-table'>${videoPage.items.map(videoView).join('')}</div>${paginationView(videoPage.pagination, params)}` : `<p class='muted'>Aucune vidéo ne correspond aux critères.</p>`}
        </section>
        <aside class='admin-panel'>
          <div class='admin-actions'>
            <h2>Assets média</h2>
            <a class='btn ghost' href='#/admin?view=jobs'>Voir les jobs</a>
          </div>
          ${assets.length ? `<div class='admin-table compact'>${assets.map(assetView).join('')}</div>` : `<p class='muted'>Aucun asset enregistré.</p>`}
        </aside>
      </div>
    </section>
  `;
}

export async function renderMediaAdminUpload() {
  stopJobsPolling();
  return `
    <section class='admin-page admin-upload-page' aria-labelledby='admin-upload-title'>
      <div class='admin-hero'>
        <p class='eyebrow'>Nouvel asset</p>
        <h1 id='admin-upload-title'>Upload vidéo</h1>
        <p class='lead'>Crée une entrée de catalogue depuis un fichier vidéo. Seuls le fichier vidéo et le titre sont obligatoires.</p>
        <div class='admin-actions'>
          <a class='btn ghost' href='#/admin'>Retour à l’administration</a>
          <a class='btn ghost' href='#/admin?view=jobs'>Jobs</a>
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
    ['durationSeconds', 'runtimeMinutes', 'releaseYear', 'genres', 'synopsis', 'tagline', 'maturityRating', 'videoTitle', 'label'].forEach((key) => {
      if (!String(body.get(key) || '').trim()) body.delete(key);
    });
    ['subtitles', 'poster', 'backdrop'].forEach((key) => {
      const file = body.get(key);
      if (!(file instanceof File) || !file.name || file.size <= 0) body.delete(key);
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
    location.hash = '#/admin?view=jobs';
    route();
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

  const refreshJobs = event.target.closest('[data-refresh-jobs]');
  if (refreshJobs) {
    event.preventDefault();
    await refreshJobsPage(true);
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

async function renderMediaAdminJobs(api) {
  jobsApi = api;
  let jobs = [];
  let status = 'Synchronisation initiale impossible.';
  try {
    jobs = await api('/api/admin/media/jobs');
    status = `Dernière synchronisation : ${formatDateTime(new Date().toISOString())}`;
  } catch {
    jobs = [];
  }
  scheduleJobsPolling(api);
  return `
    <section class='admin-page admin-jobs-page' data-admin-jobs-page aria-labelledby='admin-jobs-title'>
      <div class='admin-hero'>
        <p class='eyebrow'>Pipeline média</p>
        <h1 id='admin-jobs-title'>Jobs de transcodage</h1>
        <p class='lead'>Suit les jobs FFmpeg en cours, récents et passés sans rechargement manuel.</p>
        <div class='admin-actions'>
          <a class='btn ghost' href='#/admin'>Vidéos</a>
          <a class='btn ghost' href='#/admin/upload'>Upload</a>
          <button class='btn ghost' type='button' data-refresh-jobs>Actualiser maintenant</button>
          <span class='muted' data-jobs-status>${escapeHtml(status)}</span>
        </div>
      </div>
      ${jobsGroupsView(jobs)}
    </section>
  `;
}

function scheduleJobsPolling(api) {
  jobsApi = api;
  window.setTimeout(() => {
    if (!document.querySelector('[data-admin-jobs-page]')) return;
    startJobsPolling();
    refreshJobsPage(false);
  }, 0);
}

function startJobsPolling() {
  if (jobsPollTimer) return;
  jobsPollTimer = window.setInterval(() => refreshJobsPage(false), JOBS_POLL_INTERVAL_MS);
}

function stopJobsPolling() {
  if (jobsPollTimer) {
    window.clearInterval(jobsPollTimer);
    jobsPollTimer = null;
  }
}

async function refreshJobsPage(force) {
  const page = document.querySelector('[data-admin-jobs-page]');
  if (!page || !jobsApi) {
    stopJobsPolling();
    return;
  }
  if (jobsRefreshInFlight) return;
  if (document.hidden && !force) return;
  jobsRefreshInFlight = true;
  const status = page.querySelector('[data-jobs-status]');
  if (status && force) status.textContent = 'Synchronisation…';
  try {
    const jobs = await jobsApi('/api/admin/media/jobs');
    updateJobGroups(page, jobs);
    if (status) status.textContent = `Dernière synchronisation : ${formatDateTime(new Date().toISOString())}`;
  } catch (error) {
    if (status) status.textContent = error?.message || 'Synchronisation impossible.';
  } finally {
    jobsRefreshInFlight = false;
  }
}

function jobsGroupsView(jobs) {
  const groups = groupJobs(jobs);
  return `
    <div class='admin-grid admin-grid-wide'>
      ${jobGroupView('active', 'En cours', 'Jobs en attente ou en exécution.', groups.active)}
      ${jobGroupView('recent', 'Récents', 'Jobs terminés ou échoués dans les dernières 24 h.', groups.recent)}
    </div>
    ${jobGroupView('past', 'Passés', 'Historique plus ancien retourné par le backend.', groups.past)}
  `;
}

function jobGroupView(name, title, description, jobs) {
  return `
    <section class='admin-panel' data-job-panel='${name}'>
      <div class='admin-actions'>
        <div>
          <h2>${escapeHtml(title)} <span class='status-pill'>${jobs.length}</span></h2>
          <p class='muted'>${escapeHtml(description)}</p>
        </div>
      </div>
      <div class='admin-table' data-job-group='${name}'>${jobs.length ? jobs.map(jobView).join('') : emptyJobsView(name)}</div>
    </section>
  `;
}

function updateJobGroups(page, jobs) {
  const groups = groupJobs(jobs);
  updateJobGroup(page, 'active', groups.active);
  updateJobGroup(page, 'recent', groups.recent);
  updateJobGroup(page, 'past', groups.past);
}

function updateJobGroup(page, name, jobs) {
  const group = page.querySelector(`[data-job-group='${name}']`);
  const panel = page.querySelector(`[data-job-panel='${name}']`);
  if (!group || !panel) return;
  group.innerHTML = jobs.length ? jobs.map(jobView).join('') : emptyJobsView(name);
  const counter = panel.querySelector('.status-pill');
  if (counter) counter.textContent = String(jobs.length);
}

function groupJobs(jobs) {
  const sorted = [...(jobs || [])].sort((a, b) => jobTimestamp(b) - jobTimestamp(a));
  const active = sorted.filter(isActiveJob);
  const terminal = sorted.filter((job) => !isActiveJob(job));
  const recentThreshold = Date.now() - RECENT_JOB_WINDOW_MS;
  let recent = terminal.filter((job) => jobTimestamp(job) >= recentThreshold);
  if (!recent.length && terminal.length) recent = terminal.slice(0, Math.min(RECENT_JOB_FALLBACK_LIMIT, terminal.length));
  const recentIds = new Set(recent.map((job) => job.id));
  const past = terminal.filter((job) => !recentIds.has(job.id));
  return { active, recent, past };
}

function isActiveJob(job) {
  return ['PENDING', 'RUNNING'].includes(String(job?.status || '').toUpperCase());
}

function emptyJobsView(name) {
  const labels = {
    active: 'Aucun job en cours.',
    recent: 'Aucun job terminé récemment.',
    past: 'Aucun job passé dans la fenêtre retournée par le backend.',
  };
  return `<p class='muted'>${escapeHtml(labels[name] || 'Aucun job.')}</p>`;
}

function jobTimestamp(job) {
  return Date.parse(job?.finishedAt || job?.startedAt || job?.requestedAt || '') || 0;
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
      <a class='btn ghost' href='#/admin?view=jobs'>Jobs</a>
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
          <div class='file-duration-row'>
            <input id='upload-media' name='media' type='file' accept='${VIDEO_ACCEPT}' data-media-file required>
            <output class='duration-status' data-duration-status for='upload-media' aria-label='Durée détectée'>00:00</output>
          </div>
          <p class='field-help'>Formats acceptés : ${FORMAT_HINT}</p>
        </div>
        <section class='metadata-panel' data-metadata-panel hidden>
          <span>Métadonnées détectées</span>
          <p class='field-help' data-metadata-status></p>
          <dl data-metadata-list></dl>
        </section>
        <div class='admin-field'><label for='upload-title'>Titre</label><input id='upload-title' name='title' data-autofill-field required></div>
        <div class='admin-field'><label for='upload-year'>Année</label><input id='upload-year' name='releaseYear' type='number' min='1888' max='${DEFAULT_YEAR + 2}' placeholder='${DEFAULT_YEAR}' data-autofill-field></div>
        <div class='admin-field'><label for='upload-genres'>Genres</label><input id='upload-genres' name='genres' placeholder='Science, Drame' data-autofill-field></div>
        <div class='admin-field'><label for='upload-synopsis'>Description</label><textarea id='upload-synopsis' name='synopsis' rows='5' data-autofill-field placeholder='Optionnel'></textarea></div>
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
          <label for='upload-subtitles'>Sous-titres VTT</label>
          <input id='upload-subtitles' name='subtitles' type='file' accept='.vtt,text/vtt'>
          <p class='field-help'>Optionnel. Un fichier VTT vide sera généré si aucun sous-titre n’est fourni.</p>
        </div>
        <div class='admin-field'>
          <label for='upload-poster'>Affiche / miniature</label>
          <input id='upload-poster' name='poster' type='file' accept='image/*' data-image-preview data-preview-target='poster'>
          <div class='admin-preview-frame'><img data-preview='poster' alt='Prévisualisation de l’affiche' hidden></div>
        </div>
        <div class='admin-field thumbnail-picker'>
          <label for='thumbnail-time'>Miniature depuis la vidéo</label>
          <div class='thumbnail-controls'>
            <input id='thumbnail-time' type='number' min='0' step='0.1' placeholder='Timestamp en secondes' data-thumbnail-time disabled>
            <button class='btn ghost' type='button' data-capture-thumbnail disabled>Utiliser ce timestamp</button>
          </div>
          <p class='field-help' data-thumbnail-status>Disponible après sélection d’un fichier vidéo.</p>
        </div>
        <div class='admin-field'>
          <label for='upload-backdrop'>Arrière-plan</label>
          <input id='upload-backdrop' name='backdrop' type='file' accept='image/*' data-image-preview data-preview-target='backdrop'>
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
  setDuration(form, null);
  clearMetadata(form);
  if (!form || !file) return;

  const derivedTitle = titleFromFilename(file.name);
  const derivedYear = yearFromFilename(file.name);
  setAutoValue(form.elements.title, derivedTitle);
  setAutoValue(form.elements.videoTitle, derivedTitle);
  setAutoValue(form.elements.label, 'Film');
  if (derivedYear) setAutoValue(form.elements.releaseYear, String(derivedYear));

  probeMediaMetadata(form, file);
}

async function probeMediaMetadata(form, file) {
  const status = form.querySelector('[data-metadata-status]');
  const panel = form.querySelector('[data-metadata-panel]');
  if (panel) panel.hidden = false;
  if (status) status.textContent = 'Extraction des métadonnées…';
  const body = new FormData();
  body.append('media', file, file.name);
  try {
    const response = await fetch('/api/admin/videos/probe', { method: 'POST', credentials: 'same-origin', body });
    if (!response.ok) throw new Error('Métadonnées indisponibles.');
    const metadata = await response.json();
    setDuration(form, metadata.durationSeconds || null);
    applyMetadata(form, metadata);
    renderMetadata(form, metadata);
    if (status) status.textContent = metadata.durationSeconds ? 'Métadonnées extraites depuis le fichier.' : 'Aucune durée exploitable trouvée.';
  } catch (error) {
    setDuration(form, null);
    renderMetadata(form, { error: error.message || 'Métadonnées indisponibles.' });
    if (status) status.textContent = 'Métadonnées indisponibles.';
  }
}

function applyMetadata(form, metadata) {
  setAutoValue(form.elements.title, metadata.title);
  setAutoValue(form.elements.videoTitle, metadata.title);
  setAutoValue(form.elements.synopsis, metadata.description);
  if (metadata.releaseDate) {
    const year = String(metadata.releaseDate).match(/(?:19|20)\d{2}/)?.[0];
    if (year) setAutoValue(form.elements.releaseYear, year);
  }
}

function renderMetadata(form, metadata) {
  const panel = form.querySelector('[data-metadata-panel]');
  const list = form.querySelector('[data-metadata-list]');
  if (!panel || !list) return;
  panel.hidden = false;
  const rows = [];
  if (metadata.error) rows.push(['Statut', metadata.error]);
  if (metadata.title) rows.push(['Titre', metadata.title]);
  if (metadata.description) rows.push(['Description', metadata.description]);
  if (metadata.authors?.length) rows.push(['Auteurs', metadata.authors.join(', ')]);
  if (metadata.releaseDate) rows.push(['Date', metadata.releaseDate]);
  if (metadata.formatLongName || metadata.formatName) rows.push(['Format', metadata.formatLongName || metadata.formatName]);
  if (metadata.encoder) rows.push(['Encodeur', metadata.encoder]);
  list.innerHTML = rows.length
    ? rows.map(([label, value]) => `<div><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value)}</dd></div>`).join('')
    : '<div><dt>Statut</dt><dd>Aucune métadonnée exploitable.</dd></div>';
}

function clearMetadata(form) {
  const panel = form?.querySelector('[data-metadata-panel]');
  const list = form?.querySelector('[data-metadata-list]');
  const status = form?.querySelector('[data-metadata-status]');
  if (panel) panel.hidden = true;
  if (list) list.innerHTML = '';
  if (status) status.textContent = '';
}

function setDuration(form, seconds) {
  const duration = form?.querySelector('[data-duration-seconds]');
  const runtime = form?.querySelector('[data-runtime-minutes]');
  const thumbnailTime = form?.querySelector('[data-thumbnail-time]');
  const status = form?.querySelector('[data-duration-status]');
  const cleanSeconds = Number(seconds) > 0 ? Math.round(Number(seconds)) : null;
  if (status) status.textContent = formatClock(cleanSeconds);
  if (!duration || !runtime) return;
  if (!cleanSeconds) {
    duration.value = '';
    runtime.value = '';
    thumbnailTime?.removeAttribute('max');
    return;
  }
  duration.value = String(cleanSeconds);
  runtime.value = String(Math.max(1, Math.ceil(cleanSeconds / 60)));
  if (thumbnailTime) thumbnailTime.max = String(cleanSeconds);
}

function setThumbnailControls(form, enabled) {
  form?.querySelectorAll('[data-thumbnail-time], [data-capture-thumbnail]').forEach((element) => {
    element.disabled = !enabled;
  });
  const status = form?.querySelector('[data-thumbnail-status]');
  if (status) status.textContent = enabled ? 'Indique un timestamp en secondes puis génère l’affiche depuis la vidéo.' : 'Disponible après sélection d’un fichier vidéo.';
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
    const body = new FormData();
    body.append('media', file, file.name);
    const response = await fetch(`/api/admin/videos/thumbnail?timestampSeconds=${encodeURIComponent(requestedTime)}`, { method: 'POST', credentials: 'same-origin', body });
    if (!response.ok) throw new Error('Impossible d’extraire une image depuis ce fichier.');
    const blob = await response.blob();
    const thumbnail = new File([blob], `${filenameStem(file.name)}-thumbnail.jpg`, { type: 'image/jpeg' });
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

function updateImagePreview(input) {
  const form = input.closest('[data-upload-form]');
  const previewName = input.dataset.previewTarget;
  const image = form?.querySelector(`[data-preview='${previewName}']`);
  const file = input.files?.[0];
  if (!image) return;
  if (image.dataset.objectUrl) URL.revokeObjectURL(image.dataset.objectUrl);
  if (!file) {
    image.hidden = true;
    image.removeAttribute('src');
    return;
  }
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

function formatClock(totalSeconds) {
  const total = Math.max(0, Number(totalSeconds) || 0);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const seconds = total % 60;
  return hours > 0 ? `${hours}:${pad2(minutes)}:${pad2(seconds)}` : `${pad2(minutes)}:${pad2(seconds)}`;
}

function pad2(value) {
  return String(value).padStart(2, '0');
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
  next.delete('view');
  next.set('page', String(page));
  return `#/admin?${next.toString()}`;
}

function jobView(job) {
  const status = String(job.status || '').toLowerCase();
  const percent = Math.max(0, Math.min(100, Number(job.progressPercent) || 0));
  const force = job.force ? 'Forcé' : 'Normal';
  const message = job.message ? `<p>${escapeHtml(job.message)}</p>` : '';
  const dates = [
    `Demandé ${formatDateTime(job.requestedAt)}`,
    job.startedAt ? `Démarré ${formatDateTime(job.startedAt)}` : null,
    job.finishedAt ? `Terminé ${formatDateTime(job.finishedAt)}` : null,
    force,
  ].filter(Boolean).join(' · ');
  return `<article class='admin-row admin-job-row' data-job-id='${job.id}'><div><strong>#${job.id} · ${escapeHtml(job.title)} — ${escapeHtml(job.videoTitle)}</strong><p>${escapeHtml(dates)}</p>${message}${progressBar(percent, 'Progression ' + percent + '%')}</div><span class='status-pill status-${status}'>${escapeHtml(job.status)}</span></article>`;
}

function assetView(asset) {
  return `<article class='admin-row'><div><strong>${escapeHtml(asset.title)} — ${escapeHtml(asset.videoTitle)}</strong><p>${escapeHtml(asset.originalFilename)} · ${escapeHtml(asset.thumbnailManifestPath || 'pas de thumbnails')}</p></div><span class='status-pill status-${String(asset.status || '').toLowerCase()}'>${escapeHtml(asset.status)}</span></article>`;
}

function formatDateTime(value) {
  if (!value) return '—';
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(date);
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
