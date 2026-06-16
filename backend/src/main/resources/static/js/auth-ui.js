(() => {
  const app = document.querySelector('#app');
  let userPromise = null;
  let userCache = undefined;
  let handlingSubmit = false;

  function hasAdminRole(user) {
    return Array.isArray(user?.roles) && user.roles.includes('ADMIN');
  }

  async function loadUser(force = false) {
    if (!force && userCache !== undefined) return userCache;
    if (!force && userPromise) return userPromise;
    userPromise = fetch('/api/me', { credentials: 'same-origin', headers: { Accept: 'application/json' } })
      .then((response) => response.ok ? response.json() : null)
      .catch(() => null)
      .then((user) => {
        userCache = user;
        return user;
      })
      .finally(() => {
        userPromise = null;
      });
    return userPromise;
  }

  function escapeHtml(value) {
    return String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  function enhanceLoginPage() {
    const loginForm = document.querySelector('[data-login-form]');
    if (!loginForm || loginForm.dataset.registrationEnhanced === 'true') return;
    loginForm.dataset.registrationEnhanced = 'true';
    loginForm.insertAdjacentHTML('afterend', `
      <p class='muted'>Pas encore de compte ? <a href='#/register' data-show-register>Créer un compte utilisateur</a></p>
    `);
  }

  function renderRegister(error = '') {
    if (!app) return;
    app.innerHTML = `
      <main id='main-content' class='login-page'>
        <section class='login-card' aria-labelledby='register-title'>
          <p class='eyebrow'>Compte utilisateur</p>
          <h1 id='register-title'>Inscription</h1>
          <p class='lead'>Crée un compte simple avec le rôle USER. Ce rôle permet de consulter le catalogue et interdit l'administration.</p>
          ${error ? `<p class='error-state' role='alert'>${escapeHtml(error)}</p>` : ''}
          <form data-register-form>
            <div class='form-field'><label for='register-username'>Nom d'utilisateur</label><input id='register-username' name='username' autocomplete='username' pattern='[A-Za-z0-9_.-]{3,40}' minlength='3' maxlength='40' required></div>
            <div class='form-field'><label for='register-password'>Mot de passe</label><input id='register-password' name='password' type='password' autocomplete='new-password' minlength='8' required></div>
            <button class='btn primary' type='submit'>Créer le compte</button>
          </form>
          <p class='muted'><a href='#/' data-show-login>Retour à la connexion</a></p>
        </section>
      </main>
    `;
  }

  async function register(form) {
    const data = new FormData(form);
    const payload = {
      username: String(data.get('username') || '').trim(),
      password: String(data.get('password') || ''),
    };
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      let message = `Erreur HTTP ${response.status}`;
      try {
        const body = await response.json();
        message = body.message || message;
      } catch {
        // keep default
      }
      throw new Error(message);
    }
    userCache = await response.json().then((body) => body.user).catch(() => null);
    location.hash = '#/';
    location.reload();
  }

  async function protectAdminUi() {
    const user = await loadUser(false);
    if (!user || hasAdminRole(user)) return;
    document.querySelectorAll("a[href^='#/admin']").forEach((link) => link.remove());
    if ((location.hash || '').startsWith('#/admin')) {
      location.hash = '#/';
    }
  }

  async function sync() {
    if ((location.hash || '') === '#/register') {
      const user = await loadUser(false);
      if (user) {
        location.hash = '#/';
        return;
      }
      if (!document.querySelector('[data-register-form]')) renderRegister();
      return;
    }
    enhanceLoginPage();
    protectAdminUi();
  }

  document.addEventListener('submit', async (event) => {
    const form = event.target.closest('[data-register-form]');
    if (!form || handlingSubmit) return;
    event.preventDefault();
    handlingSubmit = true;
    try {
      await register(form);
    } catch (error) {
      renderRegister(error.message || 'Inscription impossible.');
    } finally {
      handlingSubmit = false;
    }
  });

  document.addEventListener('click', (event) => {
    if (event.target.closest('[data-show-register]')) {
      event.preventDefault();
      location.hash = '#/register';
      renderRegister();
    }
  });

  window.addEventListener('hashchange', () => window.setTimeout(sync, 0));
  new MutationObserver(() => sync()).observe(app, { childList: true, subtree: true });
  sync();
})();
