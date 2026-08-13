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
  // For every file Playwright transpiles, not only the ones under e2e/ — a spec that imports a page
  // declaration pulls app code in, and that code has to resolve the same way. See e2e/tsconfig.json
  // for the one mapping it adds.
  tsconfig: "./e2e/tsconfig.json",
  // Serially by default: the tests share one backend and one test account, and a parallel run would
  // have them fight over the same entities.
  workers: 1,
  fullyParallel: false,
  // Never in CI: without a Spring backend these tests can only fail, and a retry won't change that.
  retries: 0,
  /**
   * Well above Playwright's 5 s, because of what these tests run against: the dev server compiles a
   * route on the first navigation to it, and the assertion that follows that navigation is the one
   * waiting for it. At 5 s it is a race the slower half of a full run loses — a different handful of
   * tests each time, all of them with the same "element(s) not found" on a page that was still being
   * built. Which is the least useful kind of red: it says nothing about the code under test.
   *
   * Raising this rather than adding a longer timeout per call: the wait is a property of the
   * environment, not of any one expectation. A test that genuinely fails still fails — only later.
   */
  expect: { timeout: 20_000 },
  // And a test budget that has room for a few of those waits: at Playwright's 30 s a single route
  // compilation plus the steps after it can end the test in the middle of what it was checking, which
  // is how `cost1-edit` failed while waiting for a button that arrived a second later.
  timeout: 90_000,
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
