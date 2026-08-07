import type { NextConfig } from "next";
import { BASE_PATH } from "./lib/config";

const isProd = process.env.NODE_ENV === "production";

// No next-intl plugin: the static export has no server, so the locale and
// message catalogs are resolved on the client (see i18n/locale-provider.tsx).
const nextConfig: NextConfig = {
  // Served by Spring under /next, side-by-side with the legacy /react app.
  basePath: BASE_PATH,
  // Prod ships as a static export (no Node server); Spring serves the assets.
  //
  // Dev must NOT use it: with `output: "export"` the dev server rejects any dynamic param that
  // `generateStaticParams()` does not list, so every deep link (/next/books/5, /next/address/edit/42)
  // answers 500 - exactly the urls that need testing. Those work in prod because Spring falls back
  // to the SPA shell (404.html) and the client reads the params at runtime; the dev server has no
  // such fallback and insists on pre-rendering instead. `npm run build` (the CI gate) still runs
  // with the export on, so export-incompatible code is caught there.
  output: isProd ? "export" : undefined,
  // Emit <route>/index.html so Spring can serve clean nested paths, and give
  // deep links (e.g. /next/books/5) a canonical trailing-slash URL. Paths
  // without their own file fall back to the SPA shell (404.html) via Spring's
  // /next/** view-controller forward.
  trailingSlash: true,
  // …but without redirecting to add that slash. The rule applies to the dev proxy below too, so a
  // POST to /rs/... answered 308 and the browser then sent the whole body a second time — for a file
  // upload that is the file twice. Prod is unaffected either way (a static export has no server to
  // redirect, Spring serves the pre-rendered <route>/index.html), so switching it off makes dev
  // behave like prod rather than diverging from it.
  skipTrailingSlashRedirect: true,
  turbopack: {
    root: __dirname,
  },
  // Dev-only: proxy backend calls to Spring on :8080. `basePath: false` keeps
  // the source paths at the root (/rs, /rsPublic) instead of /next/rs, matching
  // the root-relative calls in lib/rs/client.ts. rewrites() do NOT run in the
  // static export, so prod relies on same-origin serving under Spring instead.
  async rewrites() {
    return [
      {
        source: "/rs/:path*",
        destination: "http://localhost:8080/rs/:path*",
        basePath: false,
      },
      {
        source: "/rsPublic/:path*",
        destination: "http://localhost:8080/rsPublic/:path*",
        basePath: false,
      },
    ];
  },
};

export default nextConfig;
