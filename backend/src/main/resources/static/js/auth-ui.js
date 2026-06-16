(() => {
  const app = document.querySelector('#app');
  const IDENTIFIER_PATTERN = '(?:[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}|[A-Za-z0-9_]{3,40})';
  const IDENTIFIER_HELP = 'Identifiant : adresse e-mail valide ou nom d\'utilisateur composé uniquement de lettres, chiffres et underscores.';
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

  function ensureIdentifierStyles() {
    if (document.querySelector('#streamfolio-auth-help-styles')) return;
    const style = document.createElement('style');
    style.id = 'streamfolio-auth-help-styles';
    style.textContent = `
      .identifier-label-row{display:flex;align-items:center;gap:.45rem;flex-wrap:wrap}
      .identifier-help-wrap{position:relative;display:inline-flex;align-items:center}
      .identifier-help{display:inline-flex;align-items:center;justify-content:center;width:1.15rem;height:1.15rem;border-radius:999px;background:#000;color:#fff;border:1px solid rgba(255,255,255,.35);font-size:.78rem;font-weight:800;line-height:1;cursor:help}
      .identifier-tooltip{position:absolute;left:50%;bottom:calc(100% + .55rem);transform:translateX(-50%);width:min(18rem,70vw);padding:.55rem .7rem;border-radius:.7rem;background:#000;color:#fff;box-shadow:0 18px 45px rgba(0,0,0,.45);font-size:.78rem;line-height:1.35;opacity:0;visibility:hidden;pointer-events:none;z-index:50}
      .identifier-help:hover+.identifier-tooltip,.identifier-help:focus+.identifier-tooltip{opacity:1;visibility:visible}
    `;
    document.head.appendChild(style);
  }

  function identifierHelp() {
    return `<span class='identifier-help-wrap'><span class='identifier-help' tabindex='0' aria-label='${escapeHtml(IDENTIFIER_HELP)}'>?</span><span class='identifier-tooltip' role='tooltip'>${escapeHtml(IDENTIFIER_HELP)}</span></span>`;
  }

  function identifierLabelText() {
    return `<span class='identifier-label-row'><span>Identifiant</span><span class='sr-only'> Nom d'utilisateur</span>${identifierHelp()}</span>`;
  }

  function configureIdentifierInput(input) {
    if (!input) return;
    input.type = 'text';
    input.autocomplete = 'username';
    input.pattern = IDENTIFIER_PATTERN;
    input.placeholder = 'adresse@example.dev ou nom_utilisateur';
    input.title = IDENTIFIER_HELP;
  }

  function enhanceLoginPage() {
    ensureIdentifierStyles();
    const loginForm = document.querySelector('[data-login-form]');
    if (!loginForm || loginForm.dataset.registrationEnhanced === 'true') return;
    loginForm.dataset.registrationEnhanced = 'true';
    const label = loginForm.querySelector("label[for='email']");
    if (label) label.innerHTML = identifierLabelText();
    configureIdentifierInput(loginForm.querySelector('#email'));
    loginForm.insertAdjacentHTML('afterend', `
      <p class='muted'>Pas encore de compte ? <a href='#/register' data-show-register>Créer un compte utilisateur</a></p>
    `);
  }

  function renderRegister(error = '') {
    ensureIdentifierStyles();
    if (!app) return;
    app.innerHTML = `
      <main id='main-content' class='login-page'>
        <section class='login-card' aria-labelledby='register-title'>
          <p class='eyebrow'>Compte utilisateur</p>
          <h1 id='register-title'>Inscription</h1>
          <p class='lead'>Crée un compte simple avec le rôle USER. Ce rôle permet de consulter le catalogue et interdit l'administration.</p>
          ${error ? `<p class='error-state' role='alert'>${escapeHtml(error)}</p>` : ''}
          <form data-register-form>
            <div class='form-field'><label for='register-identifier'>${identifierLabelText()}</label><input id='register-identifier' name='identifier' autocomplete='username' pattern='${IDENTIFIER_PATTERN}' placeholder='adresse@example.dev ou nom_utilisateur' title='${escapeHtml(IDENTIFIER_HELP)}' required></div>
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
      identifier: String(data.get('identifier') || '').trim(),
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

  async function protectAdminUi(forceUserRefresh = false) {
    const user = await loadUser(forceUserRefresh);
    if (!user || hasAdminRole(user)) return;
    document.querySelectorAll("a[href^='#/admin']").forEach((link) => link.remove());
    if ((location.hash || '').startsWith('#/admin')) {
      location.hash = '#/';
    }
  }

  async function sync() {
    const hash = location.hash || '';
    const shellVisible = Boolean(document.querySelector('.app-shell'));
    if (hash === '#/register') {
      const user = await loadUser(shellVisible);
      if (user) {
        location.hash = '#/';
        return;
      }
      if (!document.querySelector('[data-register-form]')) renderRegister();
      return;
    }
    enhanceLoginPage();
    protectAdminUi(shellVisible && !document.querySelector('[data-login-form]'));
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
    if (event.target.closest('[data-logout]')) {
      userCache = undefined;
      window.setTimeout(sync, 0);
    }
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
