const ASSET_VERSION = '23';
const CACHE_NAME = 'courseon-shell-v12';
const APP_SHELL = [
  `./?v=${ASSET_VERSION}`,
  `./index.html?v=${ASSET_VERSION}`,
  `./styles.css?v=${ASSET_VERSION}`,
  `./plan_logic.js?v=${ASSET_VERSION}`,
  `./app.js?v=${ASSET_VERSION}`,
  `./manifest.webmanifest?v=${ASSET_VERSION}`,
  `./icon.svg?v=${ASSET_VERSION}`,
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL)));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)))),
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const requestUrl = new URL(event.request.url);
  if (event.request.method !== 'GET' || requestUrl.origin !== self.location.origin || requestUrl.pathname.startsWith('/api/')) {
    return;
  }
  event.respondWith(
    fetch(event.request)
      .then((response) => {
        if (response.ok) {
          const copy = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
        }
        return response;
      })
      .catch(() => caches.match(event.request).then((cached) => cached || caches.match(`./index.html?v=${ASSET_VERSION}`))),
  );
});
