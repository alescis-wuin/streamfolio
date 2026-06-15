import { escapeHtml, progressBar } from './utils.js';

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
      ${uploadView()}
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

export async function handleMediaAdminSubmit(event, api, route) {
  const upload = event.target.closest('[data-upload-form]');
  if (upload) {
    event.preventDefault();
    await api('/api/admin/videos', { method: 'POST', body: new FormData(upload) });
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
    <form class='admin-form admin-filter-form' action='#/admin'>
      <label>Recherche <input name='query' type='search' value='${escapeHtml(params.get('query') || '')}' placeholder='titre, genre, fichier'></label>
      <label>Type <select name='type'><option value=''>Tous</option>${option('MOVIE', 'Films', params.get('type'))}${option('SERIES', 'Séries', params.get('type'))}</select></label>
      <label>Tri <select name='sort'>${['title,asc', 'title,desc', 'releaseYear,desc', 'videoTitle,asc', 'duration,desc', 'assetStatus,asc'].map((value) => option(value, value, params.get('sort'))).join('')}</select></label>
      <label>Taille <input name='size' type='number' min='1' max='100' value='${escapeHtml(params.get('size') || '10')}'></label>
      <input name='page' type='hidden' value='0'>
      <button class='btn primary' type='submit'>Filtrer</button>
      <a class='btn ghost' href='#/admin'>Réinitialiser</a>
    </form>
  `;
}

function uploadView() {
  return `
    <section class='admin-panel'>
      <h2>Uploader une nouvelle vidéo</h2>
      <form class='admin-form admin-upload-form' data-upload-form enctype='multipart/form-data'>
        <label>Titre <input name='title' required></label>
        <label>Année <input name='releaseYear' type='number' min='1888' value='2026' required></label>
        <label>Genres <input name='genres' placeholder='Science, Demo' required></label>
        <label>Description <textarea name='synopsis' required></textarea></label>
        <label>Tagline <input name='tagline'></label>
        <label>Durée vidéo s <input name='durationSeconds' type='number' min='1' value='60'></label>
        <label>Média <input name='media' type='file' accept='video/*,.mkv,.m4v' required></label>
        <label>Sous-titres VTT <input name='subtitles' type='file' accept='.vtt,text/vtt' required></label>
        <label>Poster <input name='poster' type='file' accept='image/*' required></label>
        <label>Arrière-plan <input name='backdrop' type='file' accept='image/*' required></label>
        <button class='btn primary' type='submit'>Créer</button>
      </form>
    </section>
  `;
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
  return `<article class='admin-row'><div><strong>#${job.id} · ${escapeHtml(job.title)} — ${escapeHtml(job.videoTitle)}</strong><p>${escapeHtml(job.message || '')}</p>${progressBar(job.progressPercent || 0, `Progression ${job.progressPercent || 0}%`)}</div><span class='status-pill status-${String(job.status || '').toLowerCase()}'>${escapeHtml(job.status)}</span></article>`;
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
