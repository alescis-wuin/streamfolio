(() => {
  const nativeFetch = window.fetch.bind(window);
  const safeMethods = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);
  const endpoint = '/api/csrf';
  let csrfState = null;
  let pendingCsrf = null;

  function requestUrl(input) {
    if (typeof input === 'string' || input instanceof URL) return new URL(input, window.location.origin);
    if (input && input.url) return new URL(input.url, window.location.origin);
    return new URL(String(input), window.location.origin);
  }

  function requestMethod(input, init = {}) {
    return String(init.method || (input instanceof Request ? input.method : 'GET') || 'GET').toUpperCase();
  }

  function isSameOrigin(input) {
    try {
      return requestUrl(input).origin === window.location.origin;
    } catch {
      return false;
    }
  }

  function readCookie(name) {
    return document.cookie
      .split(';')
      .map((part) => part.trim())
      .filter(Boolean)
      .map((part) => part.split('='))
      .find(([key]) => key === name)?.slice(1).join('=') || '';
  }

  function writeCsrfCookie(token) {
    if (!token) return;
    document.cookie = `XSRF-TOKEN=${encodeURIComponent(token)}; Path=/; SameSite=Lax`;
  }

  async function loadCsrf(force = false) {
    if (!force && csrfState?.token) return csrfState;
    if (!force && pendingCsrf) return pendingCsrf;

    pendingCsrf = nativeFetch(endpoint, {
      credentials: 'same-origin',
      headers: { Accept: 'application/json' },
    })
      .then((response) => {
        if (!response.ok) throw new Error('Impossible de récupérer le jeton CSRF.');
        return response.json();
      })
      .then((payload) => {
        csrfState = payload;
        writeCsrfCookie(payload.token);
        return payload;
      })
      .finally(() => {
        pendingCsrf = null;
      });

    return pendingCsrf;
  }

  function credentialsFor(input, init = {}) {
    if (init.credentials) return init.credentials;
    if (input instanceof Request && input.credentials) return input.credentials;
    return 'same-origin';
  }

  async function withCsrf(input, init = {}, force = false) {
    const payload = await loadCsrf(force);
    const headers = new Headers(init.headers || (input instanceof Request ? input.headers : undefined));
    const token = readCookie('XSRF-TOKEN') || payload.token;
    writeCsrfCookie(token);
    headers.set(payload.headerName || 'X-XSRF-TOKEN', decodeURIComponent(token));
    return { ...init, headers, credentials: credentialsFor(input, init) };
  }

  window.streamfolioCsrf = {
    refresh: () => loadCsrf(true),
    clear: () => {
      csrfState = null;
      pendingCsrf = null;
    },
  };

  window.fetch = async (input, init = {}) => {
    const method = requestMethod(input, init);
    if (!isSameOrigin(input) || safeMethods.has(method)) {
      return nativeFetch(input, init);
    }

    const first = await nativeFetch(input, await withCsrf(input, init, false));
    if (first.status !== 403) return first;

    return nativeFetch(input, await withCsrf(input, init, true));
  };
})();
