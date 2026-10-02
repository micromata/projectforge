import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end tests against the *running* system. There are no mocks — the point is to catch what a
 * contract read cannot, e.g. that Spring omits null fields entirely (`JsonInclude.Include.NON_NULL`),
 * so a form field arrives as `undefined` rather than null.
 *
 * The default target is Spring on :8080, serving the static export from the classpath
 * (`classpath:/static/next/`, see WebApplicationConfig): nothing is compiled during the run, and
 * editing sources cannot disturb it. The trade is that it shows the last Gradle build, not the
 * working tree — which is why the global setup refuses to run against a stale one (see
 * e2e/global-setup.ts). `npm run e2e:dev` (or `npm run e2e -- --port <n>`) targets a Next dev server
 * instead, sparing the rebuild during development; there every first navigation to a route compiles
 * it, so expect a slower and flakier full run. `E2E_BASE_URL` sets any other target.
 *
 * Both servers are never started from here: `webServer` is deliberately unset, and the tests fail
 * with a hint rather than starting them (see e2e/fixtures/auth.ts).
 */
const BASE_URL = process.env.E2E_BASE_URL ?? "http://localhost:8080";

/**
 * Which specs may run at the same time, decided by a tag on each spec's top-level `describe`.
 *
 * The tests share one backend and one account per role, and the user prefs of an account — list
 * filter, sort, grid state, page size, favorites, calendar and task-tree state — are kept by user id
 * on the server, not per session (AbstractUserPrefCache). They are kept per list, though: two specs
 * on different lists don't notice each other, two on the same one do — even one that merely resets
 * the filter in `beforeEach`, and even a passive visit, which writes the loaded filter back.
 *
 * - `@parallel`: touches no shared list state (own entities, read-only pages); any number at once.
 * - `@lane-<area>`: works on the stored state of that area; one at a time per lane, lanes side by
 *   side.
 * - `@isolated`, or **no tag at all**: changes something every other test would see (a global alert,
 *   the login penalty of the default account, two lanes at once), or hasn't been classified yet. Runs
 *   alone, after everything else (see e2e/tools/run-e2e.mjs).
 *
 * A new spec therefore runs safely without a tag, only slowly; tagging it is an opt-in.
 */
const LANES = [
  "book",
  "order",
  "task",
  "invoice",
  "creditor",
  "calendar",
  "cost1",
  "cost2",
  "customer",
  "timesheet",
];
const PARALLEL_TAGS = /@parallel|@lane-/;

/** Set by `npm run e2e -- --port <n>` / `e2e:dev` (e2e/tools/run-e2e.mjs); :3000 is the dev server's default. */
const DEV_SERVER =
  process.env.E2E_DEV_SERVER === "1" || new URL(BASE_URL).port === "3000";

export default defineConfig({
  testDir: "./e2e",
  // For every file Playwright transpiles, not only the ones under e2e/ — a spec that imports a page
  // declaration pulls app code in, and that code has to resolve the same way. See e2e/tsconfig.json
  // for the one mapping it adds.
  tsconfig: "./e2e/tsconfig.json",
  globalSetup: "./e2e/global-setup.ts",
  // The total; how they are shared out is up to the projects below. A file's tests stay together in
  // one worker (they share beforeAll data and, in places, an order).
  workers: Number(process.env.E2E_WORKERS ?? 6),
  fullyParallel: false,
  // Never in CI: without a Spring backend these tests can only fail, and a retry won't change that.
  retries: 0,
  /**
   * Above Playwright's 5 s: the backend is a real one with a production-sized database, and several
   * workers share it. Against the dev server (which compiles a route on its first navigation) the old
   * 20 s are needed — the wait is a property of the environment, not of any one expectation.
   */
  expect: { timeout: DEV_SERVER ? 20_000 : 10_000 },
  // A test budget with room for a few of those waits.
  timeout: DEV_SERVER ? 90_000 : 60_000,
  // The runner script (e2e/tools/run-e2e.mjs) overrides this with a blob reporter per phase and merges
  // both into one HTML report.
  reporter: process.env.CI ? "line" : [["list"], ["html", { open: "never" }]],
  use: {
    // Without BASE_PATH: an absolute path passed to page.goto replaces the whole path of the
    // baseURL, so a base path here would silently vanish. `goto` (e2e/fixtures/auth.ts) prefixes it.
    baseURL: BASE_URL,
    // A failed UI test is a question about what the page looked like — so keep the evidence.
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "off",
  },
  projects: [
    { name: "parallel", grep: /@parallel/ },
    ...LANES.map((lane) => ({
      name: `lane-${lane}`,
      grep: new RegExp(`@lane-${lane}\\b`),
      workers: 1,
    })),
    { name: "isolated", grepInvert: PARALLEL_TAGS, workers: 1 },
  ].map((project) => ({
    ...project,
    use: { ...devices["Desktop Chrome"] },
  })),
});
