/** Contract of org.projectforge.rest.MenuCustomizerRest; the menus themselves are `MenuItem`s of lib/rs/types. */

import type { MenuItem } from "@/lib/rs/types";

/** The top level of the user's own menu (favorites): entries and groups. */
export const FAVORITES = "favorites";

/** The template (the main menu): a drag source only, nothing is dropped into it. */
export const TEMPLATE = "template";

/**
 * What a draggable or droppable of the customizer carries (dnd-kit `data`).
 *
 * `container` is where it lives: [FAVORITES], [TEMPLATE] or the id of a group. `item` is absent on the
 * plain drop areas (the favorites panel and the body of a group), which stand for "the end of this list".
 */
export interface DragData {
  kind: "item" | "group";
  item?: MenuItem;
  container: string;
}

/** Body of `POST /rs/menu/customized` (MenuCustomizerRest.CustomMenuData). */
export interface CustomMenuData {
  favoritesMenu: MenuItem[];
}
