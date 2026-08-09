import type { Page } from "@playwright/test";
import {
  formatContextFrom,
  formatDate,
  formatTimestampMinutes,
  type FormatContext,
} from "../../lib/format";
import { MESSAGES, normalizeLocale, DEFAULT_LOCALE } from "../../i18n/config";
import type { UserData, UserStatus } from "../../lib/rs/types";

/**
 * Locale-dependent expectations of a test, derived from the logged-in user instead of hard coded.
 *
 * A test must not spell out a locale's wording or date layout: the account's language decides both,
 * so an assertion on "Ausleihen" or "dd.MM.yyyy" would pass only for a German user and hide a real
 * bug for everyone else. Labels come from the same catalogs the app renders from (i18n/config) and
 * dates through the same helpers (lib/format), which keeps the test honest about *what* is shown
 * without pinning *how*.
 */
export interface UserFormat {
  /** Formatting context of the user — locale, time zone, currency (as useFormatContext builds it). */
  context: FormatContext;
  /**
   * Looks up a dotted message key, e.g. `book.lendOut`, substituting `{argN}` placeholders — the
   * generator turns the bundle's `{0}` into `{arg0}`, so a plural label like "Letzte {arg0} Tage"
   * only matches once its argument is filled in.
   */
  t: (key: string, values?: Record<string, string | number>) => string;
  /** Formats a date the way the page does (date only, no time). */
  date: (value: unknown) => string;
  /** Formats an instant to the minute, as the table and the filter pills do. */
  timestamp: (value: unknown) => string;
}

export async function userFormat(page: Page): Promise<UserFormat> {
  const user = await fetchUserData(page);
  const context = formatContextFrom(user);
  const messages = MESSAGES[normalizeLocale(user.locale) ?? DEFAULT_LOCALE];
  return {
    context,
    t: (key, values) => fill(lookup(messages, key), values),
    date: (value) => formatDate(value, context),
    timestamp: (value) => formatTimestampMinutes(value, context),
  };
}

/** Replaces the `{argN}` placeholders next-intl fills at render time. */
function fill(
  message: string,
  values: Record<string, string | number> | undefined
): string {
  if (!values) return message;
  return Object.entries(values).reduce(
    (text, [name, value]) => text.replaceAll(`{${name}}`, String(value)),
    message
  );
}

async function fetchUserData(page: Page): Promise<UserData> {
  const status = await page.evaluate<UserStatus>(async () => {
    const response = await fetch("/rs/userStatus", {
      credentials: "include",
      headers: { "X-PF-Frontend": "next" },
    });
    return response.json();
  });
  return status.userData;
}

/** Dotted key into the nested catalogs (`book.lendOut`), as next-intl resolves them. */
function lookup(messages: Record<string, unknown>, key: string): string {
  let node: unknown = messages;
  for (const part of key.split(".")) {
    if (typeof node !== "object" || node === null) break;
    node = (node as Record<string, unknown>)[part];
  }
  if (typeof node !== "string") {
    throw new Error(`No message for "${key}" in the user's catalog.`);
  }
  return node;
}
