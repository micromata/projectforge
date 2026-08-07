import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end tests against the *running* system: the Next.js dev server on :3000, which proxies
 * `/rs` and `/rsPublic` to Spring on :8080 (see next.config.ts). There are no mocks — the point is
 * to catch what a contract read cannot, e.g. that Spring omits null fields entirely
 * (`JsonInclude.Include.NON_NULL`), so a form field arrives as `undefined` rather than null.
 *
 * Both servers have to be up; the tests fail with a hint rather than starting them (see
 * e2e/fixtures/auth.ts). `webServer` is deliberately unset: a dev server started per run would
 * recompile every route and outlast the tests' patience.
 */
export default defineConfig({
  testDir: "./e2e",
  // Serially by default: the tests share one backend and one test account, and a parallel run would
  // have them fight over the same entities.
  workers: 1,
  fullyParallel: false,
  // Never in CI: without a Spring backend these tests can only fail, and a retry won't change that.
  retries: 0,
  reporter: process.env.CI ? "line" : [["list"], ["html", { open: "never" }]],
  use: {
    // Without BASE_PATH: an absolute path passed to page.goto replaces the whole path of the
    // baseURL, so a base path here would silently vanish. `goto` (e2e/fixtures/auth.ts) prefixes it.
    baseURL: "http://localhost:3000",
    // A failed UI test is a question about what the page looked like — so keep the evidence.
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "off",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
