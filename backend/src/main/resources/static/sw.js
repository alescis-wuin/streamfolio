const CACHE_NAME = 'streamfolio-shell-v14';
const SHELL = [
  '/',
  '/index.html',
  '/styles.css',
  '/media-admin.css',
  '/ui-overrides.css',
  '/csrf.js',
  '/app.js',
  '/js/api.js',
  '/js/utils.js',
  '/js/player.js',
  '/js/media-admin.js',
  '/manifest.webmanifest',
  '/assets/icons/icon.svg',
  '/assets/posters-clean/aurora-drift.svg',
  '/assets/posters-clean/botanical-cities.svg',
  '/assets/posters-clean/silent-protocol.svg',
  '/assets/posters-clean/kitchen-orbit.svg',
  '/assets/posters-clean/neon-orchard.svg',
  '/assets/posters-clean/glass-archive.svg',
  '/assets/posters-clean/carbon-tide.svg',
  '/assets/posters-clean/quiet-circuit.svg',
  '/assets/posters-clean/nomad-frames.svg',
  '/assets/posters-clean/signal-garden.svg',
  '/assets/posters-clean/lunar-kernel.svg',
  '/assets/posters-clean/velvet-latency.svg',
  '/assets/posters-clean/moss-engine.svg',
  '/assets/posters-clean/chrome-harbor.svg',
  '/assets/posters-clean/ember-archive.svg',
  '/assets/posters-clean/orbit-bakery.svg',
  '/assets/posters-clean/tundra-signal.svg',
  '/assets/posters-clean/pixel-greenhouse.svg'
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL)));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)));
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  if (url.origin !== location.origin) return;
  if (url.pathname.startsWith('/api/')) return;
  if (event.request.method !== 'GET') return;

  event.respondWith(
    fetch(event.request, { cache: 'no-store' })
      .then((response) => {
        if (response.ok) {
          const clone = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
        }
        return response;
      })
      .catch(() => caches.match(event.request))
  );
});
