const VIDEO_ACCEPT = [
  'video/*',
  '.mp4', '.m4v', '.mov', '.qt', '.wmv', '.asf', '.mkv', '.webm', '.avi', '.divx',
  '.flv', '.f4v', '.swf', '.mts', '.m2ts', '.ts', '.m2t', '.mpeg', '.mpg', '.mpe',
  '.m1v', '.m2v', '.m2p', '.ps', '.vob', '.ogv', '.ogg', '.3gp', '.3g2', '.mxf',
  '.dv', '.rm', '.rmvb', '.mod', '.tod', '.dat'
].join(',');

const FORMAT_HINT = 'MP4, MOV, WMV, MKV, WebM/HTML5, AVI, FLV, F4V, SWF, AVCHD, MPEG-2, MPEG-TS, OGV, 3GP, MXF, DV, RealMedia, MOD/TOD, etc.';
const DEFAULT_DURATION = '00:00';

let observerStarted = false;

function enhanceUploadPage() {
  const form = document.querySelector('[data-upload-form]');
  const mediaInput = document.querySelector('#upload-media');
  if (!form || !mediaInput || mediaInput.dataset.uploadEnhanced === 'true') {
    return;
  }

  mediaInput.dataset.uploadEnhanced = 'true';
  mediaInput.setAttribute('accept', VIDEO_ACCEPT);
  const help = mediaInput.closest('.admin-field')?.querySelector('.field-help');
  if (help) {
    help.textContent = `Formats acceptés : ${FORMAT_HINT}`;
  }

  const durationStatus = moveDurationBesideInput(form, mediaInput);
  addMetadataPanel(form, mediaInput);
  mediaInput.addEventListener('change', () => handleMediaSelection(form, mediaInput, durationStatus));
  enhanceThumbnailButton(form, mediaInput);
}

function moveDurationBesideInput(form, mediaInput) {
  const existingStatus = form.querySelector('[data-duration-status]');
  const durationStatus = existingStatus || document.createElement('output');
  durationStatus.classList.add('duration-status');
  durationStatus.dataset.durationStatus = '';
  durationStatus.setAttribute('aria-label', 'Durée détectée');
  durationStatus.textContent = DEFAULT_DURATION;

  const oldParent = existingStatus?.closest('.admin-field');
  const inputParent = mediaInput.parentElement;
  const row = document.createElement('div');
  row.className = 'file-duration-row';
  inputParent.insertBefore(row, mediaInput);
  row.append(mediaInput, durationStatus);
  if (oldParent && oldParent !== inputParent) {
    oldParent.hidden = true;
  }
  return durationStatus;
}

function addMetadataPanel(form, mediaInput) {
  const field = mediaInput.closest('.admin-field');
  if (!field || form.querySelector('[data-metadata-panel]')) {
    return;
  }
  const panel = document.createElement('section');
  panel.className = 'metadata-panel';
  panel.dataset.metadataPanel = '';
  panel.hidden = true;
  panel.innerHTML = `
    <span>Métadonnées détectées</span>
    <p class='field-help' data-metadata-status>Détection via ffprobe après sélection du fichier.</p>
    <dl data-metadata-list></dl>
  `;
  field.insertAdjacentElement('afterend', panel);
}

async function handleMediaSelection(form, mediaInput, durationStatus) {
  const file = mediaInput.files?.[0];
  setDuration(form, durationStatus, null);
  setThumbnailControls(form, Boolean(file));
  clearMetadata(form);
  if (!file) {
    return;
  }

  const filenameTitle = titleFromFilename(file.name);
  setAutoValue(form.elements.title, filenameTitle);
  setAutoValue(form.elements.videoTitle, filenameTitle);
  setAutoValue(form.elements.label, 'Film');
  setAutoValue(form.elements.genres, 'Demo');
  setAutoValue(form.elements.synopsis, `Description à compléter pour ${filenameTitle}.`);
  setAutoValue(form.elements.maturityRating, 'TV-PG');
  const filenameYear = yearFromFilename(file.name);
  if (filenameYear) {
    setAutoValue(form.elements.releaseYear, String(filenameYear));
  }

  await probeMetadata(form, mediaInput, file, durationStatus);
}

async function probeMetadata(form, mediaInput, file, durationStatus) {
  const panel = form.querySelector('[data-metadata-panel]');
  const status = form.querySelector('[data-metadata-status]');
  if (panel) {
    panel.hidden = false;
  }
  if (status) {
    status.textContent = 'Extraction des métadonnées…';
  }

  const body = new FormData();
  body.append('media', file, file.name);
  try {
    const response = await fetch('/api/admin/videos/probe', {
      method: 'POST',
      credentials: 'same-origin',
      body,
    });
    if (!response.ok) {
      throw new Error('Métadonnées indisponibles.');
    }
    const metadata = await response.json();
    applyMetadata(form, metadata, durationStatus);
    renderMetadata(form, metadata);
    if (status) {
      status.textContent = metadata.durationSeconds ? 'Métadonnées extraites depuis le fichier.' : 'Aucune durée exploitable trouvée.';
    }
    mediaInput.dispatchEvent(new CustomEvent('streamfolio:metadata', { bubbles: true, detail: metadata }));
  } catch (error) {
    setDuration(form, durationStatus, null);
    renderMetadata(form, { error: error.message || 'Métadonnées indisponibles.' });
    if (status) {
      status.textContent = 'Métadonnées indisponibles.';
    }
  }
}

function applyMetadata(form, metadata, durationStatus) {
  setDuration(form, durationStatus, metadata.durationSeconds || null);
  setAutoValue(form.elements.title, metadata.title);
  setAutoValue(form.elements.videoTitle, metadata.title);
  setAutoValue(form.elements.synopsis, metadata.description);
  if (metadata.releaseDate) {
    const year = String(metadata.releaseDate).match(/(?:19|20)\d{2}/)?.[0];
    if (year) {
      setAutoValue(form.elements.releaseYear, year);
    }
  }
}

function renderMetadata(form, metadata) {
  const list = form.querySelector('[data-metadata-list]');
  if (!list) {
    return;
  }
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
  const panel = form.querySelector('[data-metadata-panel]');
  const list = form.querySelector('[data-metadata-list]');
  if (panel) {
    panel.hidden = true;
  }
  if (list) {
    list.innerHTML = '';
  }
}

function setDuration(form, durationStatus, seconds) {
  const duration = form?.querySelector('[data-duration-seconds]');
  const runtime = form?.querySelector('[data-runtime-minutes]');
  const thumbnailTime = form?.querySelector('[data-thumbnail-time]');
  const cleanSeconds = Number(seconds) > 0 ? Math.round(Number(seconds)) : null;
  if (durationStatus) {
    durationStatus.textContent = formatDuration(cleanSeconds);
  }
  if (!duration || !runtime) {
    return;
  }
  if (!cleanSeconds) {
    duration.value = '';
    runtime.value = '';
    thumbnailTime?.removeAttribute('max');
    return;
  }
  duration.value = String(cleanSeconds);
  runtime.value = String(Math.max(1, Math.ceil(cleanSeconds / 60)));
  if (thumbnailTime) {
    thumbnailTime.max = String(cleanSeconds);
  }
}

function setThumbnailControls(form, enabled) {
  form?.querySelectorAll('[data-thumbnail-time], [data-capture-thumbnail]').forEach((element) => {
    element.disabled = !enabled;
  });
  const status = form?.querySelector('[data-thumbnail-status]');
  if (status) {
    status.textContent = enabled ? 'Indique un timestamp en secondes puis génère l’affiche depuis la vidéo.' : 'Disponible après sélection d’un fichier vidéo.';
  }
}

function enhanceThumbnailButton(form, mediaInput) {
  const button = form.querySelector('[data-capture-thumbnail]');
  if (!button || button.dataset.backendThumbnail === 'true') {
    return;
  }
  button.dataset.backendThumbnail = 'true';
  button.addEventListener('click', async (event) => {
    event.preventDefault();
    event.stopPropagation();
    await captureThumbnailFromBackend(form, mediaInput, button);
  });
}

async function captureThumbnailFromBackend(form, mediaInput, button) {
  const posterInput = form.querySelector('[data-preview-target=poster]');
  const timeInput = form.querySelector('[data-thumbnail-time]');
  const status = form.querySelector('[data-thumbnail-status]');
  const file = mediaInput.files?.[0];
  if (!file || !posterInput || !timeInput) {
    return;
  }
  const requestedTime = Math.max(0, Number(timeInput.value || 0));
  if (status) {
    status.textContent = 'Extraction de l’image…';
  }
  button.disabled = true;
  try {
    const body = new FormData();
    body.append('media', file, file.name);
    const response = await fetch(`/api/admin/videos/thumbnail?timestampSeconds=${encodeURIComponent(requestedTime)}`, {
      method: 'POST',
      credentials: 'same-origin',
      body,
    });
    if (!response.ok) {
      throw new Error('Impossible d’extraire une image depuis ce fichier.');
    }
    const blob = await response.blob();
    const thumbnail = new File([blob], `${filenameStem(file.name)}-thumbnail.jpg`, { type: 'image/jpeg' });
    const transfer = new DataTransfer();
    transfer.items.add(thumbnail);
    posterInput.files = transfer.files;
    posterInput.dispatchEvent(new Event('input', { bubbles: true }));
    if (status) {
      status.textContent = `Miniature générée à ${requestedTime.toFixed(1)} s.`;
    }
  } catch (error) {
    if (status) {
      status.textContent = error.message || 'Impossible d’extraire une image depuis ce fichier.';
    }
  } finally {
    button.disabled = false;
  }
}

function setAutoValue(field, value) {
  if (!field || !value) {
    return;
  }
  if (!field.value || field.dataset.autofilled === 'true') {
    field.value = value;
    field.dataset.autofilled = 'true';
  }
}

function titleFromFilename(filename) {
  return filenameStem(filename)
    .replace(/[._-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim() || 'Nouvelle vidéo';
}

function filenameStem(filename) {
  return String(filename || 'video').replace(/\.[^.]+$/, '');
}

function yearFromFilename(filename) {
  return String(filename || '').match(/(?:19|20)\d{2}/)?.[0] || null;
}

function formatDuration(totalSeconds) {
  const total = Math.max(0, Number(totalSeconds) || 0);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const seconds = total % 60;
  if (hours > 0) {
    return `${hours}:${pad2(minutes)}:${pad2(seconds)}`;
  }
  return `${pad2(minutes)}:${pad2(seconds)}`;
}

function pad2(value) {
  return String(value).padStart(2, '0');
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/'/g, '&#39;');
}

function startObserver() {
  if (observerStarted) {
    return;
  }
  observerStarted = true;
  const app = document.querySelector('#app');
  if (app) {
    new MutationObserver(enhanceUploadPage).observe(app, { childList: true, subtree: true });
  }
  window.addEventListener('hashchange', () => setTimeout(enhanceUploadPage, 0));
  setTimeout(enhanceUploadPage, 0);
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', startObserver, { once: true });
} else {
  startObserver();
}
