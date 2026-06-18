(() => {
  const ACTIVE_JOB_STATUSES = new Set(['PENDING', 'RUNNING', 'RETRYING']);
  const RETRY_JOB_STATUSES = new Set(['FAILED', 'CANCELLED']);
  let jobsRefreshPending = false;

  document.addEventListener('click', handleClick);
  window.addEventListener('hashchange', scheduleEnhance);
  window.addEventListener('DOMContentLoaded', scheduleEnhance);

  const observer = new MutationObserver(scheduleEnhance);
  observer.observe(document.documentElement, { childList: true, subtree: true });

  function scheduleEnhance() {
    window.requestAnimationFrame(() => {
      enhanceUploadPublicationField();
      enhanceVideoDetailPublication();
      enhanceJobActions();
    });
  }

  function enhanceUploadPublicationField() {
    const form = document.querySelector('[data-upload-form]');
    if (!form || form.querySelector('[name="publicationStatus"]')) return;
    const titleField = form.querySelector('#upload-title')?.closest('.admin-field');
    const field = document.createElement('div');
    field.className = 'admin-field';
    field.innerHTML = `
      <label for="upload-publication-status">Publication</label>
      <select id="upload-publication-status" name="publicationStatus">
        <option value="PUBLISHED" selected>Publié</option>
        <option value="DRAFT">Brouillon</option>
      </select>
      <p class="field-help">Un brouillon reste visible dans l'administration mais n'est pas exposé dans le catalogue public.</p>
    `;
    titleField?.after(field);
  }

  async function enhanceVideoDetailPublication() {
    const match = String(location.hash || '').match(/#\/admin\/videos\/(\d+)/);
    if (!match) return;
    const videoId = Number(match[1]);
    const page = document.querySelector('.admin-video-detail-page');
    if (!page || page.dataset.publicationEnhanced === String(videoId)) return;
    page.dataset.publicationEnhanced = String(videoId);

    const video = await fetchJson(`/api/admin/videos/${videoId}`).catch(() => null);
    if (!video) return;
    addPublicationMetadata(page, video);
    addPublicationEditField(page, video);
  }

  function addPublicationMetadata(page, video) {
    const list = page.querySelector('.admin-detail-list');
    if (!list || list.querySelector('[data-publication-row]')) return;
    const row = document.createElement('div');
    row.dataset.publicationRow = 'true';
    row.innerHTML = `<dt>Publication</dt><dd>${escapeHtml(video.publicationStatus || 'PUBLISHED')}</dd>`;
    list.append(row);
  }

  function addPublicationEditField(page, video) {
    const form = page.querySelector('[data-admin-edit]');
    if (!form || form.querySelector('[name="publicationStatus"]')) return;
    const field = document.createElement('label');
    const value = String(video.publicationStatus || 'PUBLISHED').toUpperCase();
    field.innerHTML = `Publication <select name="publicationStatus">
      <option value="PUBLISHED"${value === 'PUBLISHED' ? ' selected' : ''}>Publié</option>
      <option value="DRAFT"${value === 'DRAFT' ? ' selected' : ''}>Brouillon</option>
    </select>`;
    form.querySelector('button[type="submit"]')?.before(field);
  }

  async function enhanceJobActions() {
    const page = document.querySelector('[data-admin-jobs-page]');
    if (!page || jobsRefreshPending) return;
    jobsRefreshPending = true;
    try {
      const jobs = await fetchJson('/api/admin/media/jobs');
      for (const job of jobs || []) {
        const row = page.querySelector(`[data-job-id="${Number(job.id)}"]`);
        if (row) addJobActions(row, job);
      }
    } finally {
      jobsRefreshPending = false;
    }
  }

  function addJobActions(row, job) {
    if (row.querySelector('[data-job-actions]')) return;
    const status = String(job.status || '').toUpperCase();
    const actions = document.createElement('div');
    actions.className = 'admin-actions';
    actions.dataset.jobActions = 'true';
    const cancel = ACTIVE_JOB_STATUSES.has(status)
      ? `<button class="btn small ghost" type="button" data-job-cancel="${Number(job.id)}">Stopper</button>`
      : '';
    const retry = RETRY_JOB_STATUSES.has(status)
      ? `<button class="btn small ghost" type="button" data-job-retry="${Number(job.id)}">Relancer</button>`
      : '';
    const attempts = Number.isFinite(Number(job.attemptCount)) ? `<span class="muted">Essais ${job.attemptCount}/${job.maxAttempts || '—'}</span>` : '';
    actions.innerHTML = `${cancel}${retry}${attempts}`;
    row.querySelector('div')?.append(actions);
  }

  async function handleClick(event) {
    const cancel = event.target.closest('[data-job-cancel]');
    if (cancel) {
      event.preventDefault();
      await postJobAction(cancel.dataset.jobCancel, 'cancel');
      return;
    }
    const retry = event.target.closest('[data-job-retry]');
    if (retry) {
      event.preventDefault();
      await postJobAction(retry.dataset.jobRetry, 'retry');
    }
  }

  async function postJobAction(jobId, action) {
    const response = await fetch(`/api/admin/media/jobs/${Number(jobId)}/${action}`, {
      method: 'POST',
      credentials: 'same-origin',
      headers: { Accept: 'application/json' },
    });
    if (!response.ok) throw new Error(`Action job impossible: ${action}`);
    document.querySelector('[data-refresh-jobs]')?.click();
    scheduleEnhance();
  }

  async function fetchJson(url) {
    const response = await fetch(url, { credentials: 'same-origin', headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('Requete admin impossible.');
    return response.json();
  }

  function escapeHtml(value) {
    return String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }
})();
