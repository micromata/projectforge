"use client";

import { useMenu } from "@/hooks/use-menu";
import type { MenuItem } from "@/lib/rs/types";

/** The id of the menu entry (`MenuItemDefId.COST_SEARCH`). */
const MENU_ID = "COST_SEARCH";

function contains(items: MenuItem[] | undefined): boolean {
  return !!items?.some((item) => item.id === MENU_ID || contains(item.subMenu));
}

/**
 * Whether the logged-in user may use the cost search: the backend sends its main menu entry only to
 * those it lets in (MenuCreator: cost 2 access or controlling group), so the floating panel and its
 * button follow the same rule without repeating it here.
 */
export function useCostSearchAccess(): boolean {
  const { data: menu } = useMenu();
  return contains(menu?.mainMenu?.menuItems);
}
