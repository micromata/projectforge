import { BASE_PATH } from "./config";

export type MenuTarget =
  /** A route of this app — navigate client-side via next/link. */
  | { kind: "internal"; href: string }
  /** Another frontend (legacy React, Wicket) or an external url — needs a full page load. */
  | { kind: "external"; href: string };

const NEXT_PREFIX = "next/";
const REACT_PREFIX = "react/";
const WICKET_PREFIX = "wa/";

/**
 * Maps a backend menu url (see MenuItemDefId in projectforge-business) onto a navigation target.
 *
 * The backend decides per menu entry which frontend serves a page, so the three apps can run side
 * by side: `next/...` belongs to this app, while `react/...` (legacy React) and `wa/...` (Wicket)
 * must be left to their own app via a full page load — client-side routing would not find them.
 */
export function resolveMenuUrl(url: string | undefined | null): MenuTarget {
  if (!url) return { kind: "internal", href: "#" };

  if (/^[a-z][a-z0-9+.-]*:\/\//i.test(url) || url.startsWith("//")) {
    return { kind: "external", href: url };
  }

  const path = url.startsWith("/") ? url.slice(1) : url;

  if (path === NEXT_PREFIX.slice(0, -1) || path.startsWith(NEXT_PREFIX)) {
    // Strip the base path: next/link prepends it again via basePath.
    const rest = path.slice(NEXT_PREFIX.length);
    // `/next//evil.example` would leave `//evil.example` here - a host, not a path of this app.
    // Today only the base path next/link prepends keeps that on this origin; don't rely on it.
    if (/^[/\\]/.test(rest)) return { kind: "internal", href: "/" };
    return { kind: "internal", href: `/${rest}` };
  }

  if (path.startsWith(REACT_PREFIX) || path.startsWith(WICKET_PREFIX)) {
    return { kind: "external", href: `/${path}` };
  }

  // Unprefixed urls (e.g. "logout") are this app's own routes.
  return { kind: "internal", href: `/${path}` };
}

/**
 * Keeps a redirect target (`?returnUrl=…` or the server's `redirectUrl`) on this
 * server. The backend hands the url back unvalidated (LoginServiceRest.getRedirectUrl
 * decodes whatever the caller stored), so anything with a scheme or a host —
 * `https://evil.example`, `//evil.example`, `/\evil.example` — is dropped and the
 * caller falls back to its own default.
 */
export function sanitizeRedirectUrl(
  url: string | undefined | null
): string | null {
  if (!url) return null;
  // Browsers strip tabs and newlines *inside* a url before parsing it, so `ja<TAB>vascript:` reaches
  // them as `javascript:` while a naive scheme check sees neither. Remove every C0 control (NUL
  // included) first and pass the stripped form on, so no later step sees them either.
  const trimmed = url.replace(/[\u0000-\u001f\u007f]/g, "").trim();
  if (!trimmed || /^[a-z][a-z0-9+.-]*:/i.test(trimmed)) return null;
  // "//host", "/\host" and "\\host" are all read as a host by browsers.
  if (/^[/\\][/\\]/.test(trimmed)) return null;
  // `/next/../..//evil.example` normalizes to `//evil.example` in the address bar - a foreign host to
  // every browser, though each segment looked like a path. Only the path part is checked: a `..` in a
  // query value is data. Relative urls stay allowed - the UILayout protocol sends them
  // (`vacationAccount/recalculate`, see PagesResolver.getDynamicPageUrl with `absolute = false`) and
  // they cannot leave this origin.
  const path = trimmed.split(/[?#]/, 1)[0];
  if (/(^|[/\\])\.\.([/\\]|$)/.test(path)) return null;
  return trimmed;
}

/** Absolute url for a full page load, including this app's base path when it targets Next. */
export function toAbsoluteUrl(target: MenuTarget): string {
  return target.kind === "internal" && !target.href.startsWith("#")
    ? `${BASE_PATH}${target.href}`
    : target.href;
}
