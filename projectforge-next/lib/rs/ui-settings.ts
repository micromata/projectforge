/**
 * Thin wrappers over the Next-only UI preferences controller (`org.projectforge.rest.UISettingsRest`).
 *
 * These are the client's own appearance settings, kept apart from "My account" (which edits the PFUserDO).
 * Persisted per user, so the choice follows the user across devices; the local `next-themes` value only
 * decides what paints before this GET returns (see hooks/use-theme-sync.ts).
 */

import { request } from "./client";

export type ThemePreference = "light" | "dark" | "system";

export interface UIThemeSettings {
  theme?: ThemePreference | null;
}

const BASE = "/rs/uiSettings";

/** The user's stored theme (`GET theme`); defaults to `"system"` server-side when nothing is stored yet. */
export function fetchThemeSetting(
  signal?: AbortSignal
): Promise<UIThemeSettings> {
  return request<UIThemeSettings>(`${BASE}/theme`, { method: "GET" }, signal);
}

/** Persists the theme (`POST theme`) and returns the canonical value the server stored. */
export function saveThemeSetting(
  theme: ThemePreference,
  signal?: AbortSignal
): Promise<UIThemeSettings> {
  return request<UIThemeSettings>(
    `${BASE}/theme`,
    { method: "POST", body: JSON.stringify({ theme }) },
    signal
  );
}
