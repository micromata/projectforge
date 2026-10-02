import { mkdirSync, readFileSync, renameSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import {
  request as apiRequest,
  type APIRequestContext,
} from "@playwright/test";
import { readCredentials, type Role } from "./credentials";

/**
 * One server session per role, logged in once per run and shared by every test of that role.
 *
 * A form login per test was the largest single cost of a run, and it bought nothing: the form itself
 * is login.spec.ts's subject, not every spec's. So the global setup logs each role in through the
 * REST login and stores the `JSESSIONID` cookie here, and `login()` (./auth.ts) hands it to a test's
 * browser context.
 *
 * Sharing a session does not share more than before: the user prefs (list filters, sort, grid state)
 * are keyed by user id on the server, not by session (AbstractUserPrefCache), so every test of an
 * account saw the same ones already. Which specs may run side by side is decided by their lane tags
 * (see playwright.config.ts), not by the session.
 */

/** Gitignored: the files hold live session cookies. */
const AUTH_DIR = join(__dirname, "..", ".auth");

function statePath(role: Role): string {
  return join(AUTH_DIR, `${role}.json`);
}

/** A cookie as Playwright's `storageState` and `addCookies` take it. */
export interface SessionCookie {
  name: string;
  value: string;
  domain: string;
  path: string;
  expires: number;
  httpOnly: boolean;
  secure: boolean;
  sameSite: "Strict" | "Lax" | "None";
}

/**
 * The address an API context has to use for `baseURL`. 127.0.0.1 rather than the configured
 * "localhost": Node resolves that to `::1` first, and the Next dev server listens on IPv4 only — the
 * browser tries both, an API context does not.
 */
export function apiBaseUrl(baseURL: string): string {
  return baseURL.replace("localhost", "127.0.0.1");
}

/**
 * A new API context, logged in as `role` through `POST /rsPublic/nextLogin` — the endpoint the login
 * form posts to as well.
 */
export async function loggedInRequest(
  baseURL: string,
  role: Role
): Promise<APIRequestContext> {
  const context = await apiRequest.newContext({ baseURL: apiBaseUrl(baseURL) });
  const { username, password } = readCredentials(role);
  const res = await context.post("/rsPublic/nextLogin", {
    data: { username, password },
  });
  // The endpoint answers a refused login with 200 as well; only the status field tells.
  const status = res.ok() ? (await res.json())?.status : undefined;
  if (status !== "SUCCESS") {
    await context.dispose();
    throw new Error(
      `Could not log in as "${role}" (HTTP ${res.status()}, status ${status}). Is ProjectForge ` +
        `on :8080, and in development mode — the mode in which it keeps ` +
        `$PROJECTFORGE_HOME/testAccounts.txt current?`
    );
  }
  return context;
}

/**
 * Logs `role` in and stores its cookies for the browser host of `baseURL`.
 *
 * The cookies are issued for the API context's host (127.0.0.1, see [apiBaseUrl]); a browser on
 * "localhost" would not send them back, so their domain is rewritten to the host the browser uses.
 * Written to a temporary file and renamed, so a worker reading the file while another one refreshes
 * it never sees half of it.
 */
export async function createSession(
  baseURL: string,
  role: Role
): Promise<SessionCookie[]> {
  const context = await loggedInRequest(baseURL, role);
  const host = new URL(baseURL).hostname;
  const cookies = (await context.storageState()).cookies.map((cookie) => ({
    ...cookie,
    domain: host,
  }));
  await context.dispose();
  mkdirSync(AUTH_DIR, { recursive: true });
  const file = statePath(role);
  const tmp = `${file}.${process.pid}.tmp`;
  writeFileSync(tmp, JSON.stringify({ cookies, origins: [] }, null, 2));
  renameSync(tmp, file);
  return cookies;
}

/** The stored cookies of `role`, or undefined if the global setup stored none. */
export function readSession(role: Role): SessionCookie[] | undefined {
  try {
    return JSON.parse(readFileSync(statePath(role), "utf8")).cookies;
  } catch {
    return undefined;
  }
}
