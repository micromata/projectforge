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

/** Width of a dashboard tile in the 12-column grid: 4, 6, 8 or 12 columns (always full width below `lg`). */
export type DashboardTileWidth = "third" | "half" | "twoThirds" | "full";
/** Height step of a dashboard tile's chart. */
export type DashboardTileHeight = "S" | "M" | "L";

/** One tile of a stored dashboard layout; unset width/height mean the tile's default. */
export interface DashboardTileLayout {
  id: string;
  width?: DashboardTileWidth | null;
  height?: DashboardTileHeight | null;
  hidden?: boolean | null;
}

/** The user's arrangement of a chart dashboard, tiles in display order; empty = defaults. */
export interface DashboardLayout {
  tiles: DashboardTileLayout[];
}

/** The stored layout of the chart dashboard `id` (`GET dashboard/{id}`), empty if never arranged. */
export function fetchDashboardLayout(
  id: string,
  signal?: AbortSignal
): Promise<DashboardLayout> {
  return request<DashboardLayout>(
    `${BASE}/dashboard/${encodeURIComponent(id)}`,
    { method: "GET" },
    signal
  );
}

/** Persists the layout of dashboard `id` (`POST dashboard/{id}`); an empty layout resets it. */
export function saveDashboardLayout(
  id: string,
  layout: DashboardLayout,
  signal?: AbortSignal
): Promise<DashboardLayout> {
  return request<DashboardLayout>(
    `${BASE}/dashboard/${encodeURIComponent(id)}`,
    { method: "POST", body: JSON.stringify(layout) },
    signal
  );
}
