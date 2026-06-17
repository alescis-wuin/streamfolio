(() => {
  const app = document.querySelector('#app');
  let scheduled = false;

  function isAdminHash() {
    return (location.hash || '').startsWith('#/admin');
  }

  function adminViewRendered() {
    return Boolean(document.querySelector('#admin-title, #admin-upload-title, #admin-video-detail-title, #admin-jobs-title'));
  }

  async function isAdminUser() {
    try {
      const response = await fetch('/api/me', {
        credentials: 'same-origin',
        headers: { Accept: 'application/json' },
      });
      if (!response.ok) return false;
      const user = await response.json();
      return Array.isArray(user.roles) && user.roles.includes('ADMIN');
    } catch {
      return false;
    }
  }

  async function recoverIfNeeded() {
    if (!isAdminHash() || adminViewRendered() || scheduled) return;
    if (!(await isAdminUser())) return;

    scheduled = true;
    window.setTimeout(() => {
      scheduled = false;
      if (isAdminHash() && !adminViewRendered()) {
        window.dispatchEvent(new HashChangeEvent('hashchange'));
      }
    }, 80);
  }

  window.addEventListener('hashchange', () => window.setTimeout(recoverIfNeeded, 0));
  if (app) {
    new MutationObserver(() => recoverIfNeeded()).observe(app, { childList: true, subtree: true });
  }
  recoverIfNeeded();
})();
