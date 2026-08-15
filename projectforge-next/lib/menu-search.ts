import type { MenuData, MenuItem } from "./rs/types";
import { matchesSearchTerm } from "@/components/data-table/filter-groups";

/** A navigable menu entry, flattened out of the menu trees for the quick access palette. */
export interface MenuEntry {
  /**
   * Identity of the entry: its url.
   *
   * Not `MenuItem.key`, although that is the backend's own key — it is not the *same* key in the
   * three trees. The main menu sends it qualified by its category (`COMMON.ADDRESS_LIST`,
   * MenuItemDef.id), the favourites and the account menu send the bare name (`ADDRESS_LIST`), so
   * deduplicating by it would leave every favourite in the palette twice. The url is what the two
   * rows actually have in common, and it is also what a recent entry has to survive as.
   */
  key: string;
  title: string;
  url: string;
  /** Translated title of the category the entry sits in — the palette's group heading. */
  category: string;
  badgeCounter?: number;
}

/**
 * Headings for the entries whose tree gives them none: the favourites are a flat row, and the
 * account entries hang below the user's name rather than below a category.
 */
export interface MenuCategoryLabels {
  mainMenu: string;
  favorites: string;
  myAccount: string;
}

/**
 * Every entry of the menu the user can navigate to, once, in menu order.
 *
 * The three trees overlap by design — a favourite is a copy of a main menu entry — so entries are
 * deduplicated by their url and the first occurrence wins. `mainMenu` therefore comes first: its
 * category is the one the user knows the entry by ("Fibu"), not "Favourites".
 *
 * Only leaves are kept (a url, as in `MenuItem.isLeaf()`), and only those the palette can actually
 * navigate to: the logout of the account menu is a `RESTCALL` whose url is not a page, and pushing
 * it as a route would log nobody out (see UserMenu, which handles it in-app).
 */
export function flattenMenuEntries(
  menu: MenuData | undefined,
  labels: MenuCategoryLabels
): MenuEntry[] {
  if (!menu) return [];
  const entries: MenuEntry[] = [];
  const seen = new Set<string>();

  const collect = (items: MenuItem[] | undefined, category: string) => {
    items?.forEach((item) => {
      if (item.subMenu?.length) {
        // A category heading of the main menu, or a folder inside the favourites: its own title is
        // what its entries are grouped under.
        collect(item.subMenu, item.title);
        return;
      }
      if (!item.url || item.type === "RESTCALL") return;
      if (seen.has(item.url)) return;
      seen.add(item.url);
      entries.push({
        key: item.url,
        title: item.title,
        url: item.url,
        category,
        badgeCounter: item.badge?.counter,
      });
    });
  };

  // The fallback headings only apply to a leaf sitting at the top level of its tree: in the main
  // menu every entry hangs below a category, and a favourite may be a folder or a bare entry.
  collect(menu.mainMenu?.menuItems, labels.mainMenu);
  collect(menu.favoritesMenu?.menuItems, labels.favorites);
  // MenuRest hangs the account entries below a single item carrying the user's full name, which is
  // no category heading — the entries are grouped under "My account" instead.
  menu.myAccountMenu?.menuItems.forEach((item) =>
    collect(item.subMenu ?? [item], labels.myAccount)
  );

  return entries;
}

/**
 * How well an entry answers the term, lowest first. Only the order of hits, never whether
 * something is one — that is [matchesSearchTerm]'s answer.
 *
 * A hit at a word start is what the user meant far more often than one in the middle: "buch" should
 * offer "Buchungskonten" before "Adressbücher", and "ad" "Adressen" before "Kreditoren-Adressen".
 * Hyphens count as word boundaries, so the second half of "Adress-buch" is a word start too.
 */
function rankOf(entry: MenuEntry, needle: string): number {
  const title = entry.title.toLowerCase();
  if (title.startsWith(needle)) return 0;
  if (new RegExp(`[\\s\\-/(]${escapeRegExp(needle)}`).test(title)) return 1;
  if (title.includes(needle)) return 2;
  // Matched by its category alone ("fibu" naming every entry of that category).
  return 3;
}

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * The entries matching the term, best first; an empty term keeps all of them in menu order.
 *
 * The category is searchable as well, which is what makes "fibu" list a whole area at once.
 */
export function searchMenuEntries(
  entries: MenuEntry[],
  term: string
): MenuEntry[] {
  const needle = term.trim().toLowerCase();
  if (!needle) return entries;
  return (
    entries
      .filter((entry) => matchesSearchTerm(needle, entry.title, entry.category))
      .map((entry, index) => ({ entry, index, rank: rankOf(entry, needle) }))
      // Ties keep menu order: with the same rank there is nothing better to go by, and a stable
      // order is what lets the user learn where an entry appears.
      .sort((a, b) => a.rank - b.rank || a.index - b.index)
      .map(({ entry }) => entry)
  );
}

/** The entries grouped by their category, in the order the entries themselves are in. */
export function groupMenuEntries(
  entries: MenuEntry[]
): { category: string; entries: MenuEntry[] }[] {
  const groups = new Map<string, MenuEntry[]>();
  entries.forEach((entry) => {
    const members = groups.get(entry.category);
    if (members) members.push(entry);
    else groups.set(entry.category, [entry]);
  });
  return [...groups].map(([category, members]) => ({
    category,
    entries: members,
  }));
}
