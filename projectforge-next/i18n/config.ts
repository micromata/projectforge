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

/**
 * Overlays a deployment's `CustomerI18nResources` onto the static catalog with highest priority.
 *
 * The static catalog is built from the product bundle only; a customer's overrides live in the backend's
 * runtime resourceDir, so the frontend fetches them (flat, dotted keys — the properties form) and applies
 * them here. Each override is written along its dotted path, mirroring the generator's own rule
 * (`GenerateNextI18nMessagesMain.JsonNode.put`): a key that is both a leaf and a namespace keeps its leaf
 * under the reserved `_`, so an override never silently drops a sibling namespace. The base is cloned, not
 * mutated — the shared `MESSAGES` stays the untouched fallback.
 */
export function applyCustomerOverrides(
  base: Record<string, unknown>,
  overrides: Record<string, string> | undefined
): Record<string, unknown> {
  if (!overrides || Object.keys(overrides).length === 0) return base;
  const root = structuredClone(base) as MessageTree;
  for (const [dottedKey, value] of Object.entries(overrides)) {
    setByDottedPath(root, dottedKey, value);
  }
  return root;
}

function setByDottedPath(root: MessageTree, dottedKey: string, value: string) {
  const parts = dottedKey.split(".");
  let node = root;
  for (const part of parts.slice(0, -1)) {
    const existing = node[part];
    if (existing !== null && typeof existing === "object") {
      node = existing;
    } else if (typeof existing === "string") {
      // A leaf that now needs children: keep it under "_", as the generator does.
      const branch: MessageTree = { _: existing };
      node[part] = branch;
      node = branch;
    } else {
      const branch: MessageTree = {};
      node[part] = branch;
      node = branch;
    }
  }
  const last = parts[parts.length - 1];
  const existing = node[last];
  if (existing !== null && typeof existing === "object") {
    // A namespace already claims this name; store the override beside it under "_".
    existing._ = value;
  } else {
    node[last] = value;
  }
}

export const LOCALE_COOKIE = "pf.locale";

// The backend sends the user's real time zone in userData.timeZone; this is only
// the prerender/bootstrap default.
export const DEFAULT_TIME_ZONE = "Europe/Berlin";

export function isLocale(value: string | undefined | null): value is Locale {
  return !!value && (LOCALES as readonly string[]).includes(value);
}

/** Maps a backend/browser locale such as "de_DE" or "en-GB" onto a supported locale. */
export function normalizeLocale(
  value: string | undefined | null
): Locale | undefined {
  if (!value) return undefined;
  const language = value.replace("_", "-").split("-")[0].toLowerCase();
  return isLocale(language) ? language : undefined;
}
