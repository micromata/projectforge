// @ts-check
/**
 * Writes `out/next-spa-shell-map.json` after `next build`.
 *
 * A static export cannot emit a file per entity, so every dynamic route is prerendered exactly once
 * from the placeholder of its `generateStaticParams` (`/books/[id]` -> `/books/new`). Spring has to
 * answer a deep link such as `/next/books/25219084` with *that* route's HTML — any other shell boots
 * the wrong page component (`404.html` renders Next's not-found page, `books/index.html` the list).
 * The mapping is derived from Next's own manifests rather than hardcoded on the Java side, so a new
 * dynamic route reaches the server by rebuilding.
 *
 * @see org.projectforge.config.WebApplicationConfig.NextSpaResourceResolver (consumer)
 */
import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { join } from "node:path";

const root = process.cwd();
const OUT_DIR = join(root, "out");
const MAP_FILE = join(OUT_DIR, "next-spa-shell-map.json");

const read = (/** @type {string} */ file) =>
  JSON.parse(readFileSync(join(root, ".next", file), "utf8"));

const routesManifest = read("routes-manifest.json");
const prerenderManifest = read("prerender-manifest.json");

// Which concrete route was prerendered for a given dynamic page, e.g. /books/[id] -> /books/new.
/** @type {Map<string, string>} */
const prerenderedBySrcRoute = new Map();
for (const [route, entry] of Object.entries(prerenderManifest.routes ?? {})) {
  const srcRoute = entry?.srcRoute;
  if (srcRoute && srcRoute !== route && !prerenderedBySrcRoute.has(srcRoute)) {
    prerenderedBySrcRoute.set(srcRoute, route);
  }
}

// The manifest order is the router's own matching order (most specific first) — keep it.
const routes = [];
for (const { page, namedRegex, regex } of routesManifest.dynamicRoutes ?? []) {
  const prerendered = prerenderedBySrcRoute.get(page);
  if (!prerendered) {
    console.warn(
      `[spa-shell-map] no prerendered route for ${page} — generateStaticParams must return one placeholder; deep links will 404`
    );
    continue;
  }
  // trailingSlash: true, so every page is a directory holding index.html next to the RSC payloads
  // the client router fetches. The directory is what Spring substitutes, which makes both work.
  const shellDir = prerendered.replace(/^\//, "");
  if (!existsSync(join(OUT_DIR, shellDir, "index.html"))) {
    console.warn(
      `[spa-shell-map] ${shellDir}/index.html missing in out/ — skipping ${page}`
    );
    continue;
  }
  // The named groups are Next's internals (nxtPid); the server only needs to know *whether* a path
  // is this route, so the plain regex is enough and keeps the Java side free of that convention.
  routes.push({ page, regex: regex ?? namedRegex, shellDir });
}

writeFileSync(MAP_FILE, `${JSON.stringify({ routes }, null, 2)}\n`);
console.log(
  `[spa-shell-map] ${routes.length} dynamic route(s) -> out/next-spa-shell-map.json`
);
