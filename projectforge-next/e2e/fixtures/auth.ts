import { test as base, expect, type Page } from "@playwright/test";
import { readCredentials } from "./credentials";
import { BASE_PATH } from "../../lib/config";

/**
 * Base test with a logged-in page.
 *
 * Every test gets its own browser context (Playwright's default), so the login runs per test rather
 * than being shared through a storage state file — the session lives in a `JSESSIONID` cookie whose
 * lifetime the tests don't control, and a stale one would fail in a way that looks like a UI bug.
 */
export const test = base.extend<{ loggedInPage: Page }>({
  loggedInPage: async ({ page }, use) => {
    await login(page);
    await use(page);
  },
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
    if (await hydrated(page)) break;
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
export async function waitForHydration(page: Page): Promise<void> {
  await page.waitForFunction(
    () =>
      Object.keys(document.querySelector("form") ?? {}).some((key) =>
        key.startsWith("__reactProps$")
      ),
    undefined,
    { timeout: 30_000 }
  );
}

/** The same wait, but as an answer rather than a failure — for the login's reload retry. */
async function hydrated(page: Page): Promise<boolean> {
  return waitForHydration(page)
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
