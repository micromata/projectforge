import type { Page } from "@playwright/test";
import { createTranslator } from "next-intl";
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
   *
   * Formatted by next-intl itself, not by a `replaceAll` of our own: the messages are ICU, so a
   * quoted apostrophe (`Feld ''{arg0}''`, as MessageFormat writes it) only becomes the single one
   * the page shows when an ICU formatter renders it.
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
  const locale = normalizeLocale(user.locale) ?? DEFAULT_LOCALE;
  return {
    context,
    t: translate(locale),
    date: (value) => formatDate(value, context),
    timestamp: (value) => formatTimestampMinutes(value, context),
  };
}

/**
 * The message lookup of one locale, for the tests that have no session to derive the user's from —
 * a failed login, a password reset. They compare against *every* shipped language, because the
 * server picks it from `Accept-Language`.
 */
export function translate(locale: keyof typeof MESSAGES): UserFormat["t"] {
  // Cast because next-intl derives the allowed keys from the *type* of the messages, and the
  // generated catalogs are plain JSON imports — a test looks its keys up as strings, exactly as the
  // dotted backend keys arrive.
  return createTranslator({
    locale,
    messages: MESSAGES[locale],
    // A missing key must fail the test loudly instead of yielding the key itself, which would make
    // an assertion pass against a page that shows nothing.
    onError: (error) => {
      throw error;
    },
  }) as unknown as UserFormat["t"];
}

/** Every shipped locale, in no particular order. */
export function locales(): (keyof typeof MESSAGES)[] {
  return Object.keys(MESSAGES) as (keyof typeof MESSAGES)[];
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
