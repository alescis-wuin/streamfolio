export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set('Accept', 'application/json');
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const response = await fetch(path, { ...options, headers, credentials: 'same-origin' });
  if (!response.ok) {
    let message = response.status >= 500 ? 'Erreur interne du serveur.' : `Erreur HTTP ${response.status}`;
    try {
      const payload = await response.json();
      message = response.status >= 500 ? 'Erreur interne du serveur.' : payload.message || message;
    } catch {
      // keep default message
    }
    throw new Error(message);
  }
  if (response.status === 204) return null;
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
