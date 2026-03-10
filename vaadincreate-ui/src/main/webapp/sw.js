const CACHE_NAME = "vaadincreate-offline-v2";
const OFFLINE_HTML = "offline.html";
const SHELL_ASSETS = [
  "offline.html",
  "VAADIN/themes/vaadincreate/styles.css",
  "VAADIN/styles/additional-styles.css",
  "VAADIN/themes/vaadincreate/images/bookstore.png"
];

function toScopedUrl(path) {
  return new URL(path, self.registration.scope).toString();
}

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) =>
      cache.addAll(SHELL_ASSETS.map((p) => new Request(toScopedUrl(p), { cache: "reload" })))
    )
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.mode !== "navigate") {
    return;
  }

  event.respondWith(
    fetch(req).catch(async () => {
      const cached = await caches.match(toScopedUrl(OFFLINE_HTML));
      return cached || Response.error();
    })
  );
});