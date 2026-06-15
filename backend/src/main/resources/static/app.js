import { api } from './js/api.js';
import { setupPlayer, playerModeView, initialPlaybackModeText } from './js/player.js';
import { renderMediaAdmin, renderMediaAdminUpload, renderMediaAdminVideoDetail, handleMediaAdminClick, handleMediaAdminInput, handleMediaAdminSubmit } from './js/media-admin.js';
import { emptyView, errorView, escapeHtml, formatDuration, labelType, loadingView, normalizeText, progressBar } from './js/utils.js';

const app = document.querySelector('#app');

const state = {
  user: null,
  playerCleanup: null,
  sectionsCache: null,
  genresCache: null,
};

const icons = {
  play: '<span aria-hidden=true>▶</span>',
  plus: '<span aria-hidden=true>＋</span>',
  check: '<span aria-hidden=true>✓</span>',
  profile: `<svg aria-hidden='true' viewBox='0 0 24 24' fill='none'><path d='M12 12.3a4.1 4.1 0 1 0 0-8.2 4.1 4.1 0 0 0 0 8.2Zm7.3 7.6c-.7-3.2-3.5-5.4-7.3-5.4s-6.6 2.2-7.3 5.4' stroke='currentColor' stroke-width='1.9' stroke-linecap='round' stroke-linejoin='round'/></svg>`,
  search: `<svg aria-hidden='true' viewBox='0 0 24 24' fill='none'><path d='m21 21-4.2-4.2m1.2-5.3a6.5 6.5 0 1 1-13 0 6.5 6.5 0 0 1 13 0Z' stroke='currentColor' stroke-width='2' stroke-linecap='round'/></svg>`,
};

const NAV_ITEMS = [
  { label: 'Accueil', href: '#/', match: (info) => info.parts.length === 0 || (info.parts[0] === 'catalog' && !info.params.toString()) },
  { label: 'Séries', href: '#/catalog?type=SERIES', match: (info) => info.parts[0] === 'catalog' && info.params.get('type') === 'SERIES' },
  { label: 'Films', href: '#/catalog?type=MOVIE', match: (info) => info.parts[0] === 'catalog' && info.params.get('type') === 'MOVIE' },
  { label: 'Ma liste', href: '#/my-list', match: (info) => info.parts[0] === 'my-list' },
  { label: 'Admin', href: '#/admin', match: (info) => info.parts[0] === 'admin' },
];

init();

async function init() {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').then((registration) => registration.update()).catch(() => {});
  }
  try {
    state.user = await api('/api/me');
  } catch {
    state.user = null;
  }
  window.addEventListener('hashchange', route);
  document.addEventListener('click', handleClick);
  document.addEventListener('submit', handleSubmit);
  document.addEventListener('change', handleInput);
  document.addEventListener('input', handleInput);
  window.addEventListener('resize', syncCarouselControls);
  route();
}

async function getSectionsData(force = false) {
  if (!force && state.sectionsCache) return state.sectionsCache;
  state.sectionsCache = await api('/api/sections');
  return state.sectionsCache;
}

async function getGenresData(force = false) {
  if (!force && state.genresCache) return state.genresCache;
  state.genresCache = await api('/api/genres').catch(() => []);
  return state.genresCache;
}

function invalidateContentCache() {
  state.sectionsCache = null;
  state.genresCache = null;
}

function routeInfo() {
  const raw = (location.hash || '#/').slice(1);
  const [path, queryString = ''] = raw.split('?');
  return {
    path: path || '/',
    parts: (path || '/').split('/').filter(Boolean),
    params: new URLSearchParams(queryString),
  };
}

async function route() {
  cleanupPlayer();
  if (!state.user) {
    renderLogin();
    return;
  }
  const info = routeInfo();
  try {
    if (info.parts[0] === 'catalog') return renderCatalog(info.params);
    if (info.parts[0] === 'my-list') return renderMyList();
    if (info.parts[0] === 'title' && info.parts[1]) return renderDetail(info.parts[1]);
    if (info.parts[0] === 'watch' && info.parts[1]) return renderPlayer(Number(info.parts[1]));
    if (info.parts[0] === 'admin' && info.parts[1] === 'upload') return renderAdminUpload();
    if (info.parts[0] === 'admin' && info.parts[1] === 'videos' && info.parts[2]) return renderAdminVideoDetailPage(Number(info.parts[2]), info.params);
    if (info.parts[0] === 'admin') return renderAdmin();
    return renderHome();
  } catch (error) {
    renderShell(errorView(error.message));
  }
}

function renderLogin(error = '') {
  app.innerHTML = `
    <main id='main-content' class='login-page'>
      <section class='login-card' aria-labelledby='login-title'>
        <p class='eyebrow'>Streamfolio portfolio</p>
        <h1 id='login-title'>Connexion</h1>
        <p class='lead'>Démo de plateforme de streaming : catalogue, progression, liste personnelle, sous-titres et lecteur vidéo via backend Java.</p>
        ${error ? `<p class='error-state' role='alert'>${escapeHtml(error)}</p>` : ''}
        <form data-login-form>
          <div class='form-field'><label for='email'>Email</label><input id='email' name='email' type='email' autocomplete='username' value='alexis@example.dev' required></div>
          <div class='form-field'><label for='password'>Mot de passe</label><input id='password' name='password' type='password' autocomplete='current-password' value='demo1234' required></div>
          <button class='btn primary' type='submit'>Entrer</button>
        </form>
      </section>
    </main>
  `;
}

function renderShell(content, options = {}) {
  const info = routeInfo();
  const searchValue = options.searchValue ?? (info.parts[0] === 'catalog' ? info.params.get('query') || '' : '');
  const nav = NAV_ITEMS.map((item) => {
    const active = item.match(info);
    return `<a class='nav-link${active ? ' active' : ''}' href='${item.href}'${active ? ' aria-current=page' : ''}>${item.label}</a>`;
  }).join('');
  const bottomNav = NAV_ITEMS.slice(0, 4).map((item) => {
    const active = item.match(info);
    return `<a class='${active ? 'active' : ''}' href='${item.href}'${active ? ' aria-current=page' : ''}>${item.label}</a>`;
  }).join('');

  app.innerHTML = `
    <div class='app-shell'>
      <header class='topbar'>
        <a class='brand' href='#/' aria-label='Retour au catalogue Streamfolio'><span class='brand-mark' aria-hidden='true'></span></a>
        <nav class='primary-nav' aria-label='Navigation principale'>${nav}</nav>
        <form class='search-form' data-search-form role='search'>
          ${icons.search}
          <label class='sr-only' for='global-search'>Rechercher</label>
          <input id='global-search' name='query' type='search' value='${escapeHtml(searchValue)}' placeholder='Titre, genre, ambiance' autocomplete='off'>
        </form>
        <div class='nav-actions'><button class='profile-button' type='button' data-logout title='Se déconnecter' aria-label='Se déconnecter du profil ${escapeHtml(state.user?.displayName || 'utilisateur')}'>${icons.profile}</button></div>
      </header>
      <main id='main-content' class='main${options.fullBleed ? ' main-full' : ''}' tabindex='-1'>${content}</main>
      <nav class='bottom-nav' aria-label='Navigation mobile'>${bottomNav}</nav>
    </div>
  `;
  requestAnimationFrame(syncCarouselControls);
}

async function renderHome() {
  renderShell(loadingView('Chargement du catalogue…'), { fullBleed: true });
  const [data, genres] = await Promise.all([getSectionsData(), getGenresData()]);
  renderRailPage({ data, genres, fullBleed: true });
}

async function renderCatalog(params) {
  const query = params.get('query') || '';
  const type = params.get('type') || '';
  const genre = params.get('genre') || '';
  renderShell(loadingView('Chargement…'), { fullBleed: true, searchValue: query });
  const [data, genres] = await Promise.all([getSectionsData(), getGenresData()]);
  renderRailPage({ data, genres, filters: { query, type, genre }, fallbackEmpty: 'Aucun titre ne correspond à ces critères.', fullBleed: true });
}

async function renderMyList() {
  renderShell(loadingView('Chargement de la liste…'), { fullBleed: true });
  const [data, genres] = await Promise.all([getSectionsData(), getGenresData()]);
  renderRailPage({ data, genres, watchlistOnly: true, fallbackEmpty: 'Utilise le bouton Ma liste sur une fiche pour ajouter un titre.', fullBleed: true });
}

async function renderAdmin() {
  renderShell(loadingView('Chargement du pipeline média…'));
  renderShell(await renderMediaAdmin(api));
}

async function renderAdminUpload() {
  renderShell(loadingView('Préparation du formulaire d’upload…'));
  renderShell(await renderMediaAdminUpload(api));
}

async function renderAdminVideoDetailPage(videoId, params) {
  renderShell(loadingView('Chargement de la vidéo admin…'));
  renderShell(await renderMediaAdminVideoDetail(api, videoId, params));
}

function renderRailPage({ data, genres, filters = {}, watchlistOnly = false, fallbackEmpty = 'Aucun contenu disponible.', fullBleed = true }) {
  const sections = buildRailSections(data.sections || [], filters, { watchlistOnly });
  const allItems = uniqueCards(sections.flatMap((section) => section.items));
  const hero = pickHero(allItems, data.hero, filters, { watchlistOnly });
  renderShell(`${hero ? heroView(hero) : ''}${discoveryView(genres, filters, { watchlistOnly })}${sections.length ? `<div class='sections'>${sections.map(sectionView).join('')}</div>` : emptyView(fallbackEmpty)}`, { fullBleed, searchValue: filters.query || '' });
}

function buildRailSections(sourceSections, filters = {}, options = {}) {
  const watchlistOnly = Boolean(options.watchlistOnly);
  const normalizedQuery = normalizeText(filters.query);
  const normalizedGenre = normalizeText(filters.genre);
  const requestedType = filters.type || '';
  const filtered = sourceSections.map((section) => {
    const items = uniqueCards((section.items || []).filter((item) => {
      if (watchlistOnly && !item.inWatchlist) return false;
      if (requestedType && item.type !== requestedType) return false;
      if (normalizedGenre && !(item.genres || []).some((genre) => normalizeText(genre) === normalizedGenre)) return false;
      if (normalizedQuery && !cardMatchesQuery(item, normalizedQuery)) return false;
      return true;
    }));
    return { ...section, items };
  }).filter((section) => section.items.length > 0);
  if (normalizedQuery || normalizedGenre || watchlistOnly) {
    const all = uniqueCards(filtered.flatMap((section) => section.items));
    return all.length ? [{ id: 'filtered', title: watchlistOnly ? 'Ma liste' : 'Résultats', description: '', items: all }] : [];
  }
  if (requestedType === 'MOVIE') return prioritizeSections(filtered, ['continue', 'watchlist', 'new', 'movies']);
  if (requestedType === 'SERIES') return prioritizeSections(filtered, ['continue', 'watchlist', 'new', 'series']);
  return filtered;
}

function sectionView(section) {
  const rowId = `row-${section.id}`;
  return `<section class='content-section' aria-labelledby='section-${escapeHtml(section.id)}'><div class='section-header'><div><h2 id='section-${escapeHtml(section.id)}'>${escapeHtml(section.title)}</h2><p>${escapeHtml(section.description)}</p></div></div><div class='carousel-shell' data-carousel-shell><button class='carousel-btn carousel-btn-left' type='button' data-scroll-target='${rowId}' data-scroll-dir='-1' aria-label='Revenir dans le carrousel ${escapeHtml(section.title)}' hidden><span class='carousel-triangle carousel-triangle-left' aria-hidden='true'></span></button><div class='card-row' data-row-id='${rowId}'>${section.items.map(cardView).join('')}</div><button class='carousel-btn carousel-btn-right' type='button' data-scroll-target='${rowId}' data-scroll-dir='1' aria-label='Avancer dans le carrousel ${escapeHtml(section.title)}' hidden><span class='carousel-triangle carousel-triangle-right' aria-hidden='true'></span></button></div></section>`;
}

function cardView(item) {
  const progress = Math.max(0, Math.min(100, item.progress?.percent || 0));
  const hasProgress = progress > 2 && !item.progress?.completed;
  return `<article class='card'><div class='card-surface'><a class='card-thumb' href='#/title/${escapeHtml(item.slug)}' aria-label='Afficher ${escapeHtml(item.title)}'><img src='${escapeHtml(item.posterPath)}' alt='' aria-hidden='true' loading='lazy'><div class='card-title-band'><h3>${escapeHtml(item.title)}</h3></div>${hasProgress ? `<div class='card-progress'>${progressBar(progress, `Progression ${progress.toFixed(0)}%`)}</div>` : ''}</a><button class='poster-play' type='button' data-play='${item.nextVideoId}' aria-label='Lire ${escapeHtml(item.title)}'><span class='play-triangle' aria-hidden='true'></span></button></div></article>`;
}

function heroView(item) {
  return `<section class='hero' aria-labelledby='hero-title'><div class='hero-backdrop' aria-hidden='true'><img src='${escapeHtml(item.backdropPath)}' alt=''></div><div class='hero-content'><h1 id='hero-title'>${escapeHtml(item.title)}</h1><p class='lead'>${escapeHtml(item.tagline)}</p>${metaRow(item)}${genreRow(item.genres)}<div class='actions'><button class='btn primary' type='button' data-play='${item.nextVideoId}'>${icons.play} ${item.progress?.positionSeconds > 0 ? 'Reprendre' : 'Lecture'}</button><a class='btn' href='#/title/${escapeHtml(item.slug)}'>Plus d’infos</a><button class='btn ghost' type='button' data-watchlist='${item.id}' data-active='${item.inWatchlist}'>${item.inWatchlist ? icons.check + ' Dans ma liste' : icons.plus + ' Ma liste'}</button></div></div></section>`;
}

function discoveryView(genres, filters = {}, options = {}) {
  if (!genres?.length) return '';
  const params = new URLSearchParams();
  if (filters.type) params.set('type', filters.type);
  if (filters.query) params.set('query', filters.query);
  const baseHref = options.watchlistOnly ? '#/my-list' : `#/catalog${params.toString() ? `?${params.toString()}` : ''}`;
  return `<section class='discovery-strip' aria-labelledby='discovery-title'><div class='discovery-panel'><div><h2 id='discovery-title'>Explorer</h2></div><div class='genre-scroll'><a class='filter-chip${!filters.genre ? ' active' : ''}' href='${baseHref}'${!filters.genre ? ' aria-current=page' : ''}>Tout afficher</a>${genres.map((genre) => { const next = new URLSearchParams(params); next.set('genre', genre); return `<a class='filter-chip${filters.genre === genre ? ' active' : ''}' href='#/catalog?${next.toString()}'${filters.genre === genre ? ' aria-current=page' : ''}>${escapeHtml(genre)}</a>`; }).join('')}</div></div></section>`;
}

async function renderDetail(slug) {
  renderShell(loadingView('Chargement de la fiche…'));
  const item = await api(`/api/catalog/${slug}`);
  const progress = Math.max(0, Math.min(100, item.progress?.percent || 0));
  const episodeSection = item.type === 'SERIES' ? `<section class='episode-list detail-glass' aria-labelledby='episodes-title'><h2 id='episodes-title'>Épisodes</h2>${item.videos.map(episodeView).join('')}</section>` : '';
  renderShell(`<section class='detail-page' aria-labelledby='detail-title'><div class='detail-backdrop' aria-hidden='true'><img src='${escapeHtml(item.backdropPath)}' alt=''></div><div class='detail-inner detail-glass'><aside class='detail-poster'><img src='${escapeHtml(item.posterPath)}' alt='Affiche de ${escapeHtml(item.title)}'></aside><div class='detail-content'><p class='eyebrow'>${labelType(item.type)}</p><h1 id='detail-title'>${escapeHtml(item.title)}</h1><p class='lead'>${escapeHtml(item.synopsis)}</p>${metaRow(item)}${genreRow(item.genres)}${progressBar(progress, `Progression ${progress.toFixed(0)}%`)}<div class='actions'><button class='btn primary' type='button' data-play='${item.nextVideoId}'>${icons.play} ${progress > 0 ? 'Reprendre' : 'Lire'}</button><button class='btn ghost' type='button' data-watchlist='${item.id}' data-active='${item.inWatchlist}' aria-label='Basculer Ma liste'>${item.inWatchlist ? icons.check + ' Ma liste' : icons.plus + ' Ma liste'}</button></div></div></div>${episodeSection}</section>`);
}

function episodeView(video) {
  const progress = Math.max(0, Math.min(100, video.progress?.percent || 0));
  return `<article class='episode'><div class='episode-index' aria-hidden='true'>${escapeHtml(video.label)}</div><div><h3>${escapeHtml(video.title)}</h3><p class='muted'>${formatDuration(video.durationSeconds)} · progression ${progress.toFixed(0)}%</p>${progressBar(progress, `Progression ${progress.toFixed(0)}%`)}</div><button class='btn small primary' type='button' data-play='${video.id}'>${icons.play} Lire</button></article>`;
}

async function renderPlayer(videoId) {
  renderShell(loadingView('Préparation du lecteur…'));
  const playback = await api(`/api/videos/${videoId}`);
  renderShell(`<section class='player-page' aria-labelledby='player-title'><div class='player-meta'><div><p class='eyebrow'>${escapeHtml(playback.label)}</p><h1 id='player-title'>${escapeHtml(playback.videoTitle)}</h1><p class='muted'>${escapeHtml(playback.title)} · ${labelType(playback.type)} · ${playback.releaseYear} · ${escapeHtml(playback.maturityRating)}</p></div><a class='btn ghost' href='#/title/${escapeHtml(playback.titleSlug)}'>Retour à la fiche</a></div><div class='video-frame'><video id='video-player' controls playsinline preload='metadata'><track src='${escapeHtml(playback.subtitlesUrl)}' kind='subtitles' srclang='fr' label='Français' default>Votre navigateur ne supporte pas la vidéo HTML5.</video></div><div id='timeline-thumbnails' class='timeline-thumbnails'></div><div class='player-mode' id='player-mode' role='status' aria-live='polite'>${playerModeView('Initialisation', initialPlaybackModeText(playback))}</div><div class='player-meta'><p class='muted'>Progression sauvegardée automatiquement.</p><div class='kbd-list' aria-label='Raccourcis clavier'><span><kbd>k</kbd> lecture/pause</span><span><kbd>←</kbd><kbd>→</kbd> ±10 s</span><span><kbd>m</kbd> muet</span><span><kbd>f</kbd> plein écran</span></div></div></section>`);
  setupPlayer(playback, { api, setCleanup: (cleanup) => { state.playerCleanup = cleanup; } });
}

async function handleSubmit(event) {
  if (await handleMediaAdminSubmit(event, api, route)) return;
  const loginForm = event.target.closest('[data-login-form]');
  if (loginForm) {
    event.preventDefault();
    const formData = new FormData(loginForm);
    try {
      const response = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ email: formData.get('email'), password: formData.get('password') }) });
      state.user = response.user;
      invalidateContentCache();
      location.hash = '#/';
      route();
    } catch (error) {
      renderLogin(error.message);
    }
    return;
  }
  const searchForm = event.target.closest('[data-search-form]');
  if (searchForm) {
    event.preventDefault();
    const info = routeInfo();
    const params = info.parts[0] === 'catalog' ? new URLSearchParams(info.params) : new URLSearchParams();
    const query = String(new FormData(searchForm).get('query') || '').trim();
    if (query) params.set('query', query); else params.delete('query');
    location.hash = `#/catalog${params.toString() ? `?${params.toString()}` : ''}`;
  }
}

async function handleClick(event) {
  if (await handleMediaAdminClick(event, api, route)) return;
  const logoutButton = event.target.closest('[data-logout]');
  if (logoutButton) { event.preventDefault(); await logout(true); return; }
  const scrollButton = event.target.closest('[data-scroll-target]');
  if (scrollButton) { const row = [...document.querySelectorAll('[data-row-id]')].find((element) => element.dataset.rowId === scrollButton.getAttribute('data-scroll-target')); if (row) scrollCarousel(row, Number(scrollButton.getAttribute('data-scroll-dir')) || 1); return; }
  const playButton = event.target.closest('[data-play]');
  if (playButton) { const id = playButton.getAttribute('data-play'); if (id && id !== 'null' && id !== 'undefined') location.hash = `#/watch/${id}`; return; }
  const watchlistButton = event.target.closest('[data-watchlist]');
  if (watchlistButton) {
    const id = watchlistButton.getAttribute('data-watchlist');
    const active = watchlistButton.getAttribute('data-active') === 'true';
    watchlistButton.disabled = true;
    try { await api(`/api/titles/${id}/watchlist`, { method: active ? 'DELETE' : 'POST' }); invalidateContentCache(); route(); } catch (error) { renderShell(errorView(error.message)); }
  }
}

function handleInput(event) {
  handleMediaAdminInput(event);
}

async function logout(refresh = true) {
  try { await api('/api/auth/logout', { method: 'POST' }); } catch {}
  state.user = null;
  invalidateContentCache();
  if (refresh) { location.hash = '#/'; route(); }
}

function prioritizeSections(sections, orderedIds) {
  const byId = new Map(sections.map((section) => [section.id, section]));
  return [...orderedIds.map((id) => byId.get(id)).filter(Boolean), ...sections.filter((section) => !orderedIds.includes(section.id))];
}

function pickHero(items, fallbackHero, filters = {}, options = {}) {
  if (items.length) return items[0];
  if (options.watchlistOnly) return null;
  if (filters.query || filters.genre || filters.type) return null;
  return fallbackHero || null;
}

function uniqueCards(items) {
  const seen = new Set();
  const result = [];
  for (const item of items || []) {
    if (!item || seen.has(item.id)) continue;
    seen.add(item.id);
    result.push(item);
  }
  return result;
}

function cardMatchesQuery(item, normalizedQuery) {
  return [item.title, item.tagline, item.synopsis, labelType(item.type), ...(item.genres || [])].map(normalizeText).join(' ').includes(normalizedQuery);
}

function metaRow(item) {
  return `<div class='meta-row' aria-label='Métadonnées'><strong>${labelType(item.type)}</strong><span aria-hidden='true'>•</span><span>${item.releaseYear}</span><span aria-hidden='true'>•</span><span>${escapeHtml(item.maturityRating)}</span><span aria-hidden='true'>•</span><span>${escapeHtml(contentLengthLabel(item))}</span></div>`;
}

function genreRow(genres = []) {
  return `<div class='genre-row'>${genres.map((genre) => `<span class='pill'>${escapeHtml(genre)}</span>`).join('')}</div>`;
}

function syncCarouselControls() {
  document.querySelectorAll('[data-carousel-shell]').forEach((shell) => {
    const row = shell.querySelector('[data-row-id]');
    if (!row) return;
    const hasOverflow = row.scrollWidth > row.clientWidth + 2;
    shell.classList.toggle('is-scrollable', hasOverflow);
    shell.querySelectorAll('[data-scroll-target]').forEach((button) => { button.hidden = !hasOverflow; });
  });
}

function scrollCarousel(row, direction) {
  const maxScroll = Math.max(0, row.scrollWidth - row.clientWidth);
  const threshold = 8;
  if (maxScroll <= threshold) return;
  if (direction < 0 && row.scrollLeft <= threshold) return row.scrollTo({ left: maxScroll, behavior: 'smooth' });
  if (direction > 0 && row.scrollLeft >= maxScroll - threshold) return row.scrollTo({ left: 0, behavior: 'smooth' });
  row.scrollBy({ left: direction * row.clientWidth * 0.86, behavior: 'smooth' });
}

function contentLengthLabel(item) {
  if (item.type === 'SERIES') {
    const seasons = Number(item.seasonCount || 0);
    const episodes = Number(item.episodeCount || 0);
    const seasonText = seasons > 0 ? `${seasons} saison${seasons > 1 ? 's' : ''}` : 'série';
    const episodeText = episodes > 0 ? `${episodes} épisode${episodes > 1 ? 's' : ''}` : '';
    return [seasonText, episodeText].filter(Boolean).join(' · ');
  }
  const runtime = Number(item.runtimeMinutes || 0);
  return runtime > 0 ? `${runtime} min` : 'durée non renseignée';
}

function cleanupPlayer() {
  if (state.playerCleanup) {
    state.playerCleanup();
    state.playerCleanup = null;
  }
}
