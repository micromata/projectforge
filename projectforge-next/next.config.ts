import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";
import { BASE_PATH } from "./lib/config";

const withNextIntl = createNextIntlPlugin("./i18n/request.ts");

const nextConfig: NextConfig = {
  // Served by Spring under /next, side-by-side with the legacy /react app.
  basePath: BASE_PATH,
  // Prod ships as a static export (no Node server); Spring serves the assets.
  output: "export",
  // Emit <route>/index.html so Spring can serve clean nested paths, and give
  // deep links (e.g. /next/books/5) a canonical trailing-slash URL. Paths
  // without their own file fall back to the SPA shell (404.html) via Spring's
  // /next/** view-controller forward.
  trailingSlash: true,
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

export default withNextIntl(nextConfig);
