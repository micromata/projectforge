/**
 * The menu customizer (`org.projectforge.rest.MenuCustomizerRest`, page `/next/customizeMenu`). The current
 * menus come from `fetchMenu` (client.ts); this is only what the customizer writes or offers in addition.
 */

import { request } from "./client";
import type { MenuItem } from "./types";
import type { CustomMenuData } from "@/components/features/menu-customizer/types";

/** Stores [favoritesMenu] as the user's own menu. */
export async function saveCustomMenu(favoritesMenu: MenuItem[]): Promise<void> {
  const body: CustomMenuData = { favoritesMenu };
  await request("/rs/menu/customized", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/** The default favorites menu, as a starting point — nothing is stored. */
export async function fetchDefaultMenu(
  signal?: AbortSignal
): Promise<MenuItem[]> {
  const data = await request<Partial<CustomMenuData>>(
    "/rs/menu/default",
    { method: "GET" },
    signal
  );
  return data.favoritesMenu ?? [];
}

/** Drops the user's own menu, so the default one applies again. */
export async function resetCustomMenu(): Promise<void> {
  await request("/rs/menu/reset", { method: "POST", body: "{}" });
}
