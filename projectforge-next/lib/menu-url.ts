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
    return { kind: "internal", href: `/${rest}` };
  }

  if (path.startsWith(REACT_PREFIX) || path.startsWith(WICKET_PREFIX)) {
    return { kind: "external", href: `/${path}` };
  }

  // Unprefixed urls (e.g. "logout") are this app's own routes.
  return { kind: "internal", href: `/${path}` };
}

/** Absolute url for a full page load, including this app's base path when it targets Next. */
export function toAbsoluteUrl(target: MenuTarget): string {
  return target.kind === "internal" && !target.href.startsWith("#")
    ? `${BASE_PATH}${target.href}`
    : target.href;
}
