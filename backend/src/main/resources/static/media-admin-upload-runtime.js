(() => {
  const ACCEPT = [
    'video/*',
    '.mp4', '.m4v', '.mov', '.qt', '.wmv', '.asf', '.mkv', '.webm', '.avi', '.divx',
    '.flv', '.f4v', '.swf', '.mts', '.m2ts', '.ts', '.m2t', '.mpeg', '.mpg', '.mpe',
    '.m1v', '.m2v', '.m2p', '.ps', '.vob', '.ogv', '.ogg', '.3gp', '.3g2', '.mxf',
    '.dv', '.rm', '.rmvb', '.mod', '.tod', '.dat'
  ].join(',');
  const FORMAT_HINT = 'MP4, MOV, WMV, MKV, WebM/HTML5, AVI, FLV, F4V, SWF, AVCHD, MPEG-2, MPEG-TS, OGV, 3GP, MXF, DV, RealMedia, MOD/TOD, etc.';

  function enhanceUpload() {
    const form = document.querySelector('[data-upload-form]');
    const input = form?.querySelector('#upload-media');
    const status = form?.querySelector('[data-duration-status]');
    if (!form || !input || !status) return;

    input.setAttribute('accept', ACCEPT);
    const help = input.closest('.admin-field')?.querySelector('.field-help');
    if (help) help.textContent = `Formats acceptés : ${FORMAT_HINT}`;

    placeDurationBesideFile(input, status);
    if (!form.querySelector('[data-duration-seconds]')?.value) status.textContent = '00:00';
    ensureMetadataPanel(input);

    if (input.dataset.runtimeUploadBound !== 'true') {
      input.dataset.runtimeUploadBound = 'true';
      input.addEventListener('change', () => setTimeout(() => probeSelectedFile(form, input), 0));
    }
    bindThumbnail(form, input);
  }

  function placeDurationBesideFile(input, status) {
    if (input.parentElement?.classList.contains('file-duration-row')) return;
    const parent = input.parentElement;
    if (!parent) return;
    const oldField = status.closest('.admin-field');
    const row = document.createElement('div');
    row.className = 'file-duration-row';
    parent.insertBefore(row, input);
    row.append(input, status);
    if (oldField && oldField !== parent) oldField.hidden = true;
  }

  function ensureMetadataPanel(input) {
    const field = input.closest('.admin-field');
    const form = input.closest('[data-upload-form]');
    if (!field || form.querySelector('[data-metadata-panel]')) return;
    const panel = document.createElement('section');
    panel.className = 'metadata-panel';
    panel.dataset.metadataPanel = '';
    panel.hidden = true;
    panel.innerHTML = "<span>Métadonnées détectées</span><p class='field-help' data-metadata-status></p><dl data-metadata-list></dl>";
    field.insertAdjacentElement('afterend', panel);
  }

  async function probeSelectedFile(form, input) {
    const file = input.files?.[0];
    const status = form.querySelector('[data-duration-status]');
    setDuration(form, 0);
    clearMetadata(form);
    if (!file) return;

    try {
      const body = new FormData();
      body.append('media', file, file.name);
      const response = await fetch('/api/admin/videos/probe', { method: 'POST', credentials: 'same-origin', body });
      if (!response.ok) throw new Error('Métadonnées indisponibles.');
      const metadata = await response.json();
      setDuration(form, metadata.durationSeconds || 0);
      applyMetadata(form, metadata);
      renderMetadata(form, metadata);
    } catch (error) {
      if (status) status.textContent = '00:00';
      renderMetadata(form, { error: error.message || 'Métadonnées indisponibles.' });
    }
  }

  function setDuration(form, seconds) {
    const clean = Number(seconds) > 0 ? Math.round(Number(seconds)) : 0;
    const status = form.querySelector('[data-duration-status]');
    const duration = form.querySelector('[data-duration-seconds]');
    const runtime = form.querySelector('[data-runtime-minutes]');
    const thumbnailTime = form.querySelector('[data-thumbnail-time]');
    if (status) status.textContent = formatDuration(clean);
    if (!duration || !runtime) return;
    duration.value = clean > 0 ? String(clean) : '';
    runtime.value = clean > 0 ? String(Math.max(1, Math.ceil(clean / 60))) : '';
    if (thumbnailTime) {
      if (clean > 0) thumbnailTime.max = String(clean);
      else thumbnailTime.removeAttribute('max');
    }
  }

  function applyMetadata(form, metadata) {
    setAutoValue(form.elements.title, metadata.title);
    setAutoValue(form.elements.videoTitle, metadata.title);
    setAutoValue(form.elements.synopsis, metadata.description);
    const year = String(metadata.releaseDate || '').match(/(?:19|20)\d{2}/)?.[0];
    if (year) setAutoValue(form.elements.releaseYear, year);
  }

  function renderMetadata(form, metadata) {
    const panel = form.querySelector('[data-metadata-panel]');
    const list = form.querySelector('[data-metadata-list]');
    const status = form.querySelector('[data-metadata-status]');
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
    list.innerHTML = rows.length ? rows.map(([key, value]) => `<div><dt>${escapeHtml(key)}</dt><dd>${escapeHtml(value)}</dd></div>`).join('') : '<div><dt>Statut</dt><dd>Aucune métadonnée exploitable.</dd></div>';
    if (status) status.textContent = metadata.error ? 'Métadonnées indisponibles.' : 'Métadonnées extraites depuis le fichier.';
  }

  function clearMetadata(form) {
    const panel = form.querySelector('[data-metadata-panel]');
    const list = form.querySelector('[data-metadata-list]');
    if (panel) panel.hidden = true;
    if (list) list.innerHTML = '';
  }

  function bindThumbnail(form, input) {
    const button = form.querySelector('[data-capture-thumbnail]');
    if (!button || button.dataset.runtimeThumbnailBound === 'true') return;
    button.dataset.runtimeThumbnailBound = 'true';
    button.addEventListener('click', async (event) => {
      event.preventDefault();
      event.stopImmediatePropagation();
      await captureThumbnail(form, input, button);
    }, true);
  }

  async function captureThumbnail(form, input, button) {
    const file = input.files?.[0];
    const posterInput = form.querySelector("input[name='poster']");
    const timeInput = form.querySelector('[data-thumbnail-time]');
    const status = form.querySelector('[data-thumbnail-status]');
    if (!file || !posterInput || !timeInput) return;
    const timestamp = Math.max(0, Number(timeInput.value || 0));
    if (status) status.textContent = 'Extraction de l’image…';
    button.disabled = true;
    try {
      const body = new FormData();
      body.append('media', file, file.name);
      const response = await fetch(`/api/admin/videos/thumbnail?timestampSeconds=${encodeURIComponent(timestamp)}`, { method: 'POST', credentials: 'same-origin', body });
      if (!response.ok) throw new Error('Impossible d’extraire une image depuis ce fichier.');
      const blob = await response.blob();
      const thumbnail = new File([blob], `${file.name.replace(/\.[^.]+$/, '')}-thumbnail.jpg`, { type: 'image/jpeg' });
      const transfer = new DataTransfer();
      transfer.items.add(thumbnail);
      posterInput.files = transfer.files;
      posterInput.dispatchEvent(new Event('input', { bubbles: true }));
      if (status) status.textContent = `Miniature générée à ${timestamp.toFixed(1)} s.`;
    } catch (error) {
      if (status) status.textContent = error.message || 'Impossible d’extraire une image depuis ce fichier.';
    } finally {
      button.disabled = false;
    }
  }

  function setAutoValue(field, value) {
    if (!field || !value) return;
    if (!field.value || field.dataset.autofilled === 'true') {
      field.value = value;
      field.dataset.autofilled = 'true';
    }
  }

  function formatDuration(seconds) {
    const total = Math.max(0, Number(seconds) || 0);
    const hours = Math.floor(total / 3600);
    const minutes = Math.floor((total % 3600) / 60);
    const rest = total % 60;
    return hours > 0 ? `${hours}:${pad(minutes)}:${pad(rest)}` : `${pad(minutes)}:${pad(rest)}`;
  }

  function pad(value) {
    return String(value).padStart(2, '0');
  }

  function escapeHtml(value) {
    return String(value ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/'/g, '&#39;');
  }

  new MutationObserver(enhanceUpload).observe(document.documentElement, { childList: true, subtree: true });
  document.addEventListener('DOMContentLoaded', enhanceUpload, { once: true });
  window.addEventListener('hashchange', () => setTimeout(enhanceUpload, 0));
  setTimeout(enhanceUpload, 0);
})();
