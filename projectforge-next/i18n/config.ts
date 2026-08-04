import de from "../messages/de.json";
import en from "../messages/en.json";

export const LOCALES = ["de", "en"] as const;
export type Locale = (typeof LOCALES)[number];

export const DEFAULT_LOCALE: Locale = "de";

// Bundled statically (a few KB each) because the static export has no server to
// resolve message catalogs per request.
export const MESSAGES: Record<Locale, Record<string, unknown>> = { de, en };

export const LOCALE_COOKIE = "pf.locale";

// The backend sends the user's real time zone in userData.timeZone; this is only
// the prerender/bootstrap default.
export const DEFAULT_TIME_ZONE = "Europe/Berlin";

export function isLocale(value: string | undefined | null): value is Locale {
  return !!value && (LOCALES as readonly string[]).includes(value);
}

/** Maps a backend/browser locale such as "de_DE" or "en-GB" onto a supported locale. */
export function normalizeLocale(value: string | undefined | null): Locale | undefined {
  if (!value) return undefined;
  const language = value.replace("_", "-").split("-")[0].toLowerCase();
  return isLocale(language) ? language : undefined;
}
