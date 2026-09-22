// Single source of truth for the base path under which this app is served.
//
// projectforge-next runs side-by-side with the legacy React app (`/react`)
// and Wicket (`/wa`), all served by the one Spring Boot app. This app owns
// `/next`. Spring forwards `/next/**` to `next-app.html` (see
// WebApplicationConfig / Constants.NEXT_APP_PATH in the backend).
//
// Note: API calls to the Spring backend (`/rs`, `/rsPublic`) are always
// root-relative (NOT prefixed with BASE_PATH), because Spring serves them at
// the origin root, not under `/next`. In dev, next.config.ts rewrites proxy
// them to :8080 (with `basePath: false` so the source stays `/rs/*`); in the
// static-export prod build they hit the same Spring origin directly.
export const BASE_PATH = "/next";
