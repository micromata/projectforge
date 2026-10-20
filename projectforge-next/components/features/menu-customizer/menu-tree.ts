import { arrayMove } from "@dnd-kit/sortable";
import type { MenuItem } from "@/lib/rs/types";
import { FAVORITES, TEMPLATE, type DragData } from "./types";

/**
 * Pure operations on the user's own menu: a list of entries and groups, a group holding entries only (one
 * level, as the favorites bar renders it). Every function returns a new list and leaves its input alone;
 * one that changes nothing returns the very same list, so the caller can tell.
 *
 * A container is [FAVORITES] (the top level) or the id of a group.
 */

/** The identity of an entry: `id` for the menu's own entries and the groups, `key` as fallback. */
export function itemId(item: MenuItem | undefined): string {
  return item?.id ?? item?.key ?? "";
}

export function isGroup(item: MenuItem): boolean {
  return Array.isArray(item.subMenu);
}

/** Whether the entry is anywhere in the menu, at the top level or in a group: it may be there only once. */
export function containsItem(menu: MenuItem[], id: string): boolean {
  return menu.some(
    (item) =>
      itemId(item) === id ||
      (item.subMenu ?? []).some((sub) => itemId(sub) === id)
  );
}

function entriesOf(
  menu: MenuItem[],
  container: string
): MenuItem[] | undefined {
  if (container === FAVORITES) return menu;
  return menu.find((item) => itemId(item) === container)?.subMenu;
}

function withEntries(
  menu: MenuItem[],
  container: string,
  entries: MenuItem[]
): MenuItem[] {
  if (container === FAVORITES) return entries;
  return menu.map((item) =>
    itemId(item) === container ? { ...item, subMenu: entries } : item
  );
}

function indexIn(menu: MenuItem[], container: string, id: string): number {
  return (
    entriesOf(menu, container)?.findIndex((item) => itemId(item) === id) ?? -1
  );
}

/** Inserts [item] at [index] of [container] (at its end for a negative index). */
export function insertItem(
  menu: MenuItem[],
  container: string,
  item: MenuItem,
  index = -1
): MenuItem[] {
  const entries = entriesOf(menu, container);
  if (!entries || containsItem(menu, itemId(item))) return menu;
  const next = [...entries];
  next.splice(index < 0 ? next.length : index, 0, { ...item });
  return withEntries(menu, container, next);
}

/** Removes the entry [id] from [container]; removing a group from the top level removes its entries too. */
export function removeItem(
  menu: MenuItem[],
  id: string,
  container: string = FAVORITES
): MenuItem[] {
  const entries = entriesOf(menu, container);
  if (!entries) return menu;
  return withEntries(
    menu,
    container,
    entries.filter((item) => itemId(item) !== id)
  );
}

export function addGroup(
  menu: MenuItem[],
  title: string,
  id: string
): MenuItem[] {
  return [...menu, { id, title, subMenu: [] }];
}

export function renameGroup(
  menu: MenuItem[],
  id: string,
  title: string
): MenuItem[] {
  return menu.map((item) => (itemId(item) === id ? { ...item, title } : item));
}

/**
 * The result of dropping [active] onto [over] (what dnd-kit reports at the end of a drag; no [over]
 * means outside of every drop area):
 * - from the template: added where it was dropped, unless it is in the menu already,
 * - within one container: moved to the drop position (onto the container itself: to its end),
 * - into another container: moved there. Groups stay at the top level, nothing goes into the template.
 */
export function applyDrop(
  menu: MenuItem[],
  active: DragData,
  over: DragData | undefined
): MenuItem[] {
  if (!over || !active.item) return menu;
  const target = over.container;
  if (target === TEMPLATE) return menu;
  if (active.kind === "group" && target !== FAVORITES) return menu;
  const id = itemId(active.item);
  const overIndex = over.item ? indexIn(menu, target, itemId(over.item)) : -1;

  if (active.container === TEMPLATE) {
    return insertItem(menu, target, active.item, overIndex);
  }
  if (active.container === target) {
    const entries = entriesOf(menu, target);
    if (!entries) return menu;
    const from = indexIn(menu, target, id);
    const to = overIndex >= 0 ? overIndex : entries.length - 1;
    if (from < 0 || from === to) return menu;
    return withEntries(menu, target, arrayMove(entries, from, to));
  }
  const removed = removeItem(menu, id, active.container);
  if (removed === menu || !entriesOf(removed, target)) return menu;
  return insertItem(removed, target, active.item, overIndex);
}

/**
 * The menu as `POST /rs/menu/customized` takes it: entries without `subMenu`, groups with their entries.
 * An empty group is left out — the backend would store it as an entry without a url.
 */
export function cleanForSave(menu: MenuItem[]): MenuItem[] {
  return menu.flatMap((item) => {
    const subMenu = item.subMenu;
    const entry = withoutSubMenu(item);
    if (!subMenu) return [entry];
    if (subMenu.length === 0) return [];
    return [{ ...entry, subMenu: subMenu.map(withoutSubMenu) }];
  });
}

function withoutSubMenu(item: MenuItem): MenuItem {
  const entry = { ...item };
  delete entry.subMenu;
  return entry;
}
