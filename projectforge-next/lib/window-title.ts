/** The name every window title ends with, and the whole title while a page has none (yet). */
export const APP_TITLE = "ProjectForge";

/**
 * The browser title of a page: what the page is called, then the application — `Kunden – ProjectForge`.
 *
 * The page first, unlike Wicket's `ProjectForge - Kunden`: with many tabs open only the beginning of
 * each title is visible, and that is the part that tells them apart.
 */
export function windowTitle(title?: string | null): string {
  const trimmed = title?.trim();
  return trimmed ? `${trimmed} – ${APP_TITLE}` : APP_TITLE;
}
