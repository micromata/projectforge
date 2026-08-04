import de from "../messages/de.json";
import en from "../messages/en.json";
// Generated from the backend's I18nResources bundle by GenerateNextI18nMessagesMain
// (part of DevelopmentMainForRelease) — do not edit by hand.
import generatedDe from "../messages/generated.de.json";
import generatedEn from "../messages/generated.en.json";

export const LOCALES = ["de", "en"] as const;
export type Locale = (typeof LOCALES)[number];

export const DEFAULT_LOCALE: Locale = "de";

type MessageTree = { [key: string]: string | MessageTree };

/**
 * Deep merge so a hand-written namespace adds to the generated one instead of
 * replacing it: `filter` holds both backend operator labels and frontend-only
 * texts. Hand-written values win on conflict.
 */
function mergeMessages(base: MessageTree, override: MessageTree): MessageTree {
  const result: MessageTree = { ...base };
  for (const [key, value] of Object.entries(override)) {
    const existing = result[key];
    result[key] =
      typeof value === "object" && typeof existing === "object"
        ? mergeMessages(existing, value)
        : value;
  }
  return result;
}

// Bundled statically (a few KB each) because the static export has no server to
// resolve message catalogs per request.
export const MESSAGES: Record<Locale, Record<string, unknown>> = {
  de: mergeMessages(generatedDe as MessageTree, de as MessageTree),
  en: mergeMessages(generatedEn as MessageTree, en as MessageTree),
};

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
