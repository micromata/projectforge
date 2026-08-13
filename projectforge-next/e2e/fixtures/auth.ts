import {
  test as base,
  expect,
  request as apiRequest,
  type APIRequestContext,
  type Page,
} from "@playwright/test";
import { readCredentials } from "./credentials";
import { BASE_PATH } from "../../lib/config";
import {
  createBook,
  createCost1,
  createOrder,
  createTask,
  type SeededBook,
  type SeededCost1,
  type SeededOrder,
  type SeededTask,
} from "./seed";

/**
 * Base test with a logged-in page, and the test data a spec asks for.
 *
 * Every test gets its own browser context (Playwright's default), so the login runs per test rather
 * than being shared through a storage state file — the session lives in a `JSESSIONID` cookie whose
 * lifetime the tests don't control, and a stale one would fail in a way that looks like a UI bug.
 *
 * The seeded entities are **worker-scoped**: one book, one cost unit and one task per run, not per
 * test. They are inserts into a real database (and cannot be removed again — see ./seed.ts), so
 * creating them per test would add a row for every case that merely reads one.
 */
export const test = base.extend<
  { loggedInPage: Page },
  {
    /** A logged-in API context, for the seeds — they run before any page exists. */
    seedRequest: APIRequestContext;
    /** A book of the tests' own, with the field shape the book specs assert on. */
    seededBook: SeededBook;
    /** A cost unit of the tests' own, whose number is free. */
    seededCost1: SeededCost1;
    /** A task of the tests' own with one child, so the tree has a collapsible node. */
    seededTask: SeededTask;
    /** An order of the tests' own, whose title is wider than any column shows. */
    seededOrder: SeededOrder;
  }
>({
  loggedInPage: async ({ page }, use) => {
    await login(page);
    await use(page);
  },

  seedRequest: [
    // `baseURL` is read off the project's `use` block rather than taken as a fixture: that one is
    // test-scoped, and a worker fixture may not depend on it.
    async ({}, use, workerInfo) => {
      const context = await apiRequest.newContext({
        // 127.0.0.1 rather than the configured "localhost": Node resolves that to `::1` first, and
        // the dev server listens on IPv4 only — the browser tries both, an API context does not.
        baseURL: (workerInfo.project.use.baseURL ?? "").replace(
          "localhost",
          "127.0.0.1"
        ),
      });
      const { username, password } = readCredentials();
      // The REST login rather than the form: this context has no browser, and the session cookie is
      // all the seeds need (see CLAUDE.md, "Testing against the running system").
      const res = await context.post("/rsPublic/nextLogin", {
        data: { username, password },
      });
      if (!res.ok()) {
        throw new Error(
          `Could not log in to create the test data (HTTP ${res.status()}). Is ProjectForge on ` +
            `:8080 and are the credentials in ~/ProjectForge/testAccount.txt current?`
        );
      }
      await use(context);
      await context.dispose();
    },
    { scope: "worker" },
  ],

  seededBook: [
    async ({ seedRequest }, use) => {
      await use(await createBook(seedRequest));
    },
    { scope: "worker" },
  ],

  seededCost1: [
    async ({ seedRequest }, use) => {
      await use(await createCost1(seedRequest));
    },
    { scope: "worker" },
  ],

  seededTask: [
    async ({ seedRequest }, use) => {
      await use(await createTask(seedRequest));
    },
    { scope: "worker" },
  ],

  seededOrder: [
    async ({ seedRequest }, use) => {
      await use(await createOrder(seedRequest));
    },
    { scope: "worker" },
  ],
});

export { expect };

/**
 * Logs in through the real login form, so the test exercises the same path a user takes (and the
 * backend gets its session cookie the way it expects).
 *
 * @param returnUrl Where the login should return to, as `?returnUrl=`. Defaults to this app's start
 *   page: without it the server sends the user to `/react/calendar` (the default of
 *   `LoginServiceRest.getRedirectUrl`), which the Next dev server on :3000 cannot serve - the test
 *   would then start on its 404 page.
 */
export async function login(page: Page, returnUrl = "/next/"): Promise<void> {
  const { username, password } = readCredentials();
  const path = `/login?returnUrl=${encodeURIComponent(returnUrl)}`;
  // Reloaded rather than awaited longer: the dev server occasionally serves a truncated chunk
  // (ERR_CONTENT_LENGTH_MISMATCH), and a page whose script never arrived will not hydrate however
  // long the test waits. A second request gets a whole one.
  for (let attempt = 1; ; attempt++) {
    await goto(page, path);
    // A short wait per attempt on purpose: the fixture has the test's own budget, so three waits of
    // the full length would run out before the second request is even sent. The route is compiled
    // once per dev-server start, and this is the route every test begins with.
    if (await hydrated(page, 10_000)) break;
    if (attempt === 3) {
      throw new Error(
        "The login page never hydrated. Is the Next dev server still compiling?"
      );
    }
  }
  await page.fill("#username", username);
  await page.fill("#password", password);
  await page.locator('button[type="submit"]').click();
  // The login redirects to the start page; waiting for the url to leave /login is what tells the
  // test the session exists. A failed login stays on /login and shows an alert instead.
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 });
}

/**
 * Waits until React has hydrated the form of the current page.
 *
 * Without it a filled and submitted form is a race: the typed values never reach React's state and
 * no submit handler is attached yet, so the browser performs the native submit and reloads the page
 * with empty fields — which looks exactly like a rejected login. Waiting for a request of the page
 * is not enough: it may be answered before hydration, and `/password-forgotten` issues none at all.
 *
 * `__reactProps$…` is React's own marker, attached to a DOM node as it is hydrated. Internal, but it
 * is the only direct signal — everything else (a visible button, an awaited response) is present in
 * the server-rendered markup already.
 */
export async function waitForHydration(
  page: Page,
  timeout = 30_000
): Promise<void> {
  await page.waitForFunction(
    () =>
      Object.keys(document.querySelector("form") ?? {}).some((key) =>
        key.startsWith("__reactProps$")
      ),
    undefined,
    { timeout }
  );
}

/** The same wait, but as an answer rather than a failure — for the login's reload retry. */
async function hydrated(page: Page, timeout: number): Promise<boolean> {
  return waitForHydration(page, timeout)
    .then(() => true)
    .catch(() => false);
}

/**
 * Navigates to an app path, failing with a usable message when nothing is listening.
 *
 * The path is app-relative ("/book/1"): BASE_PATH is prepended here rather than living in the
 * config's `baseURL`, because an absolute path given to page.goto replaces the base URL's entire
 * path — a `/next` there would be dropped and every navigation would land on Next's 404.
 *
 * These tests need two servers (`npm run dev` and Spring on :8080). Without the hint, the first
 * failure is an opaque `net::ERR_CONNECTION_REFUSED` in whichever test happened to run first.
 */
export async function goto(page: Page, path: string): Promise<void> {
  try {
    await page.goto(`${BASE_PATH}${path}`, { waitUntil: "domcontentloaded" });
  } catch (cause) {
    throw new Error(
      `Cannot reach ${path}. These tests run against the live system: start the Next dev server ` +
        `(npm run dev) and ProjectForge on :8080.`,
      { cause }
    );
  }
}
