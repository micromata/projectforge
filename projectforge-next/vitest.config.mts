import { defineConfig } from "vitest/config";

/**
 * Unit tests for the pure modules under `lib/` and the pure parts of `components/` — the logic that
 * has no DOM and no backend: date and time-zone arithmetic, filter value plumbing, formatting.
 *
 * Deliberately not a component test runner. Anything that renders is covered by Playwright against
 * the live backend (`npm run e2e`), which answers the question these tests cannot — whether the
 * request the component builds is one Spring actually accepts.
 *
 * `.mts` so Vite loads it as ESM natively; `resolve.tsconfigPaths` supplies the `@/*` alias from
 * tsconfig.json, so imports read the same here as in the app.
 */
export default defineConfig({
  resolve: { tsconfigPaths: true },
  test: {
    // Co-located with the module under test. `e2e/` stays Playwright's — its `test` import comes
    // from @playwright/test and would fail here.
    include: ["{lib,components,hooks,app,i18n,store}/**/*.test.ts"],
    environment: "node",
  },
});
