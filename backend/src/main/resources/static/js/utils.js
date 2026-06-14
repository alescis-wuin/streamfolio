export function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll(String.fromCharCode(34), '&quot;')
    .replaceAll("'", '&#039;');
}

export function normalizeText(value) {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}

export function formatDuration(seconds) {
  const value = Math.max(0, Number(seconds) || 0);
  const minutes = Math.floor(value / 60);
  const remaining = value % 60;
  return minutes === 0 ? `${remaining} s` : `${minutes} min ${String(remaining).padStart(2, '0')} s`;
}

export function labelType(type) {
  return type === 'SERIES' ? 'Série' : 'Film';
}

export function progressBar(progress, label) {
  const safeProgress = Math.max(0, Math.min(100, Number(progress) || 0));
  return `<div class='progress-track' aria-label='${escapeHtml(label)}'><span style='--progress:${safeProgress}%'></span></div>`;
}

export function loadingView(message) {
  return `<div class='loading'>${escapeHtml(message)}</div>`;
}

export function emptyView(message) {
  return `<div class='empty-state'>${escapeHtml(message)}</div>`;
}

export function errorView(message) {
  return `<div class='error-state' role='alert'><h2>Erreur</h2><p>${escapeHtml(message)}</p><div class='actions'><a class='btn' href='#/'>Revenir à l'accueil</a></div></div>`;
}
