import { escapeHtml, progressBar } from './utils.js';

export async function renderMediaAdmin(api) {
  const [jobs, assets] = await Promise.all([
    api('/api/admin/media/jobs').catch(() => []),
    api('/api/admin/media/assets').catch(() => []),
  ]);
  return `
    <section class='admin-page' aria-labelledby='admin-title'>
      <div class='admin-hero'>
        <p class='eyebrow'>Pipeline média</p>
        <h1 id='admin-title'>Transcodage</h1>
        <p class='lead'>Lance un transcodage HLS local, observe les statuts, et vérifie les assets générés.</p>
        <form class='admin-form' data-transcode-form>
          <label>Vidéo ID <input name='videoId' type='number' min='1' value='1' required></label>
          <label class='checkline'><input name='force' type='checkbox'> Régénérer</label>
          <button class='btn primary' type='submit'>Lancer le job</button>
          <button class='btn ghost' type='button' data-refresh-admin>Rafraîchir</button>
        </form>
      </div>
      <div class='admin-grid'>
        <section class='admin-panel'>
          <h2>Jobs récents</h2>
          ${jobs.length ? `<div class='admin-table'>${jobs.map(jobView).join('')}</div>` : `<p class='muted'>Aucun job pour le moment.</p>`}
        </section>
        <section class='admin-panel'>
          <h2>Assets média</h2>
          ${assets.length ? `<div class='admin-table'>${assets.map(assetView).join('')}</div>` : `<p class='muted'>Aucun asset enregistré.</p>`}
        </section>
      </div>
    </section>
  `;
}

export async function handleMediaAdminSubmit(event, api, route) {
  const form = event.target.closest('[data-transcode-form]');
  if (!form) return false;
  event.preventDefault();
  const data = new FormData(form);
  const videoId = Number(data.get('videoId')) || 1;
  const force = data.get('force') === 'on';
  await api(`/api/admin/media/videos/${videoId}/transcode`, {
    method: 'POST',
    body: JSON.stringify({ force }),
  });
  setTimeout(route, 350);
  return true;
}

export async function handleMediaAdminClick(event, route) {
  const refresh = event.target.closest('[data-refresh-admin]');
  if (!refresh) return false;
  event.preventDefault();
  route();
  return true;
}

function jobView(job) {
  return `
    <article class='admin-row'>
      <div>
        <strong>#${job.id} · ${escapeHtml(job.title)} — ${escapeHtml(job.videoTitle)}</strong>
        <p>${escapeHtml(job.message || '')}</p>
        ${progressBar(job.progressPercent || 0, `Progression ${job.progressPercent || 0}%`)}
      </div>
      <span class='status-pill status-${String(job.status || '').toLowerCase()}'>${escapeHtml(job.status)}</span>
    </article>
  `;
}

function assetView(asset) {
  return `
    <article class='admin-row'>
      <div>
        <strong>${escapeHtml(asset.title)} — ${escapeHtml(asset.videoTitle)}</strong>
        <p>${escapeHtml(asset.originalFilename)} · ${escapeHtml(asset.thumbnailManifestPath || 'pas de thumbnails')}</p>
      </div>
      <span class='status-pill status-${String(asset.status || '').toLowerCase()}'>${escapeHtml(asset.status)}</span>
    </article>
  `;
}
