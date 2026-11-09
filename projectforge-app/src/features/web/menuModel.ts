/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

import type { MenuItem, Menus } from "@projectforge/shared/rs/menu";

/** A menu entry the menu of the pages tab can open: a page of the server or another site. */
export interface MenuEntry {
  /** Its identity: the url (the key is qualified in the main menu only, see flattenMenuEntries). */
  key: string;
  title: string;
  /** As the server sends it: a path without the leading slash (`next/address`) or another site. */
  url: string;
  /** The key the use is reported by (POST /rs/menu/recent). */
  menuKey?: string;
  /** The title of the category the entry is in. */
  category: string;
  badgeCounter?: number;
}

/** The headings of the entries whose tree has none, and of the sections. */
export interface MenuLabels {
  mainMenu: string;
  favorites: string;
  myAccount: string;
  recent: string;
}

/** A row of the menu list. */
export type MenuRow =
  | { kind: "heading"; key: string; title: string; sub?: boolean }
  | { kind: "entry"; key: string; entry: MenuEntry; indent?: boolean }
  /** A category of the main menu, opened by a tap. */
  | {
      kind: "group";
      key: string;
      title: string;
      open: boolean;
      badgeCounter?: number;
    }
  /** Searching all data (next's search page) for the term. */
  | { kind: "searchData"; key: string; term: string }
  | { kind: "noMatch"; key: string }
  /** A tab without entries. */
  | { kind: "empty"; key: string }
  /** The choice of the theme (the web pages have it in the user menu). */
  | { kind: "theme"; key: string };

/** The sections of the menu, one shown at a time. */
export type MenuTab = "favorites" | "recent" | "main" | "account";

/** A page to open, null for an item that is none: a folder, or the logout (a REST call, the app has its own). */
function entryOf(item: MenuItem, category: string): MenuEntry | null {
  if (!item.url || item.type === "RESTCALL" || item.subMenu?.length)
    return null;
  return {
    key: item.url,
    title: item.title ?? item.url,
    url: item.url,
    menuKey: item.key ?? undefined,
    category,
    badgeCounter: item.badge?.counter ?? undefined,
  };
}

/** The entries of the account menu: below the one item with the user's name. */
function accountItems(menus: Menus): MenuItem[] {
  return menus.myAccountMenu.menuItems.flatMap(
    (item) => item.subMenu ?? [item],
  );
}

/** The user's full name, the title of the account menu (as the web pages show it at the right). */
export function accountTitle(menus: Menus): string | undefined {
  return menus.myAccountMenu.menuItems[0]?.title ?? undefined;
}

/**
 * Every entry the user can open, once, in menu order: the main menu first (its category is the one the user knows the
 * entry by), then the favorites and the account menu. Deduplicated by url, as the trees overlap by design and their
 * keys differ (`COMMON.ADDRESS_LIST` in the main menu, `ADDRESS_LIST` elsewhere). Port of next's
 * `lib/menu-search.ts`.
 */
export function flattenMenuEntries(
  menus: Menus,
  labels: MenuLabels,
): MenuEntry[] {
  const entries: MenuEntry[] = [];
  const seen = new Set<string>();
  const collect = (items: MenuItem[] | null | undefined, category: string) => {
    items?.forEach((item) => {
      if (item.subMenu?.length) {
        collect(item.subMenu, item.title ?? category);
        return;
      }
      const entry = entryOf(item, category);
      if (entry == null || seen.has(entry.url)) return;
      seen.add(entry.url);
      entries.push(entry);
    });
  };
  collect(menus.mainMenu.menuItems, labels.mainMenu);
  collect(menus.favoritesMenu.menuItems, labels.favorites);
  collect(accountItems(menus), labels.myAccount);
  return entries;
}

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** How well an entry answers [needle], lowest first: the start of the title, of a word, anywhere, the category. */
function rankOf(entry: MenuEntry, needle: string): number {
  const title = entry.title.toLowerCase();
  if (title.startsWith(needle)) return 0;
  if (new RegExp(`[\\s\\-/(]${escapeRegExp(needle)}`).test(title)) return 1;
  if (title.includes(needle)) return 2;
  return 3;
}

/** The entries matching [term] in their title or category, best first (ties in menu order); all for no term. */
export function searchMenuEntries(
  entries: MenuEntry[],
  term: string,
): MenuEntry[] {
  const needle = term.trim().toLowerCase();
  if (!needle) return entries;
  return entries
    .filter(
      (it) =>
        it.title.toLowerCase().includes(needle) ||
        it.category.toLowerCase().includes(needle),
    )
    .map((entry, index) => ({ entry, index, rank: rankOf(entry, needle) }))
    .sort((a, b) => a.rank - b.rank || a.index - b.index)
    .map(({ entry }) => entry);
}

/** The entries grouped by their category, in the order of the entries. */
export function groupMenuEntries(
  entries: MenuEntry[],
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

/** The tab shown first: the favorites, the main menu for a user without any. */
export function defaultTab(menus: Menus): MenuTab {
  return menus.favoritesMenu.menuItems.length > 0 ? "favorites" : "main";
}

/**
 * The badge of [tab]: the main menu sums the badges of its categories (each the sum of its entries, by the server); the
 * other tabs sum those of their entries, in folders too, each page once.
 */
export function tabBadge(menus: Menus, tab: MenuTab): number {
  if (tab === "main")
    return menus.mainMenu.menuItems.reduce(
      (sum, it) => sum + (it.subMenu?.length ? (it.badge?.counter ?? 0) : 0),
      0,
    );
  const items =
    tab === "favorites"
      ? menus.favoritesMenu.menuItems
      : tab === "recent"
        ? (menus.recentMenu?.menuItems ?? [])
        : accountItems(menus);
  const seen = new Set<string>();
  const sum = (list: MenuItem[] | null | undefined): number =>
    (list ?? []).reduce((total, item) => {
      if (item.subMenu?.length) return total + sum(item.subMenu);
      const entry = entryOf(item, "");
      if (entry == null || seen.has(entry.url)) return total;
      seen.add(entry.url);
      return total + (entry.badgeCounter ?? 0);
    }, 0);
  return sum(items);
}

/**
 * The rows of the menu: while searching the hits of all tabs by category and the search in all data; otherwise the
 * rows of [tab]: the favorites (a folder as a sub heading), the entries opened last, the categories of the main menu
 * (the open ones with their entries: as in [groupStates] if the user toggled them, else [groupsOpenByDefault]) or the
 * account menu below the user's name.
 */
export function menuRows(
  menus: Menus,
  labels: MenuLabels,
  opts: {
    term: string;
    groupStates: Readonly<Record<string, boolean>>;
    groupsOpenByDefault: boolean;
    tab: MenuTab;
  },
): MenuRow[] {
  const rows: MenuRow[] = [];
  const term = opts.term.trim();
  if (term) {
    const groups = groupMenuEntries(
      searchMenuEntries(flattenMenuEntries(menus, labels), term),
    );
    groups.forEach((group) => {
      rows.push({
        kind: "heading",
        key: `hit-heading:${group.category}`,
        title: group.category,
      });
      group.entries.forEach((entry) =>
        rows.push({ kind: "entry", key: `hit:${entry.key}`, entry }),
      );
    });
    if (groups.length === 0) rows.push({ kind: "noMatch", key: "noMatch" });
    rows.push({ kind: "searchData", key: "searchData", term });
    return rows;
  }
  const section = (
    name: string,
    title: string,
    items: MenuItem[] | null | undefined,
  ) => {
    const seen = new Set<string>();
    const add = (
      list: MenuItem[] | null | undefined,
      category: string,
      indent: boolean,
    ) =>
      list?.forEach((item) => {
        if (item.subMenu?.length) {
          rows.push({
            kind: "heading",
            key: `${name}-heading:${item.key ?? item.title}`,
            title: item.title ?? "",
            sub: true,
          });
          add(item.subMenu, item.title ?? category, true);
          return;
        }
        const entry = entryOf(item, category);
        if (entry == null || seen.has(entry.url)) return;
        seen.add(entry.url);
        rows.push({
          kind: "entry",
          key: `${name}:${entry.key}`,
          entry,
          indent,
        });
      });
    add(items, title, false);
  };
  const withEmpty = (): MenuRow[] =>
    rows.some((it) => it.kind === "entry" || it.kind === "group")
      ? rows
      : [{ kind: "empty", key: "empty" }];
  if (opts.tab === "favorites") {
    section("favorites", labels.favorites, menus.favoritesMenu.menuItems);
    return withEmpty();
  }
  if (opts.tab === "recent") {
    section("recent", labels.recent, menus.recentMenu?.menuItems);
    return withEmpty();
  }
  if (opts.tab === "account") {
    const name = accountTitle(menus);
    if (name)
      rows.push({ kind: "heading", key: "account-heading", title: name });
    section("account", labels.myAccount, accountItems(menus));
    rows.push({ kind: "theme", key: "theme" });
    return rows;
  }
  menus.mainMenu.menuItems
    .filter((it) => it.subMenu?.length)
    .forEach((category) => {
      const id = category.key ?? category.title ?? "";
      const open = opts.groupStates[id] ?? opts.groupsOpenByDefault;
      rows.push({
        kind: "group",
        key: id,
        title: category.title ?? "",
        open,
        badgeCounter: category.badge?.counter ?? undefined,
      });
      if (!open) return;
      flattenMenuEntries(
        {
          mainMenu: { menuItems: [category] },
          favoritesMenu: { menuItems: [] },
          myAccountMenu: { menuItems: [] },
        },
        labels,
      ).forEach((entry) =>
        rows.push({
          kind: "entry",
          key: `main:${id}:${entry.key}`,
          entry,
          indent: true,
        }),
      );
    });
  return withEmpty();
}

/**
 * [rows] in at most [count] columns of about the same height, read top to bottom and then left to right. A heading or a
 * category stays together with its entries; no column is empty.
 */
export function menuColumns(rows: MenuRow[], count: number): MenuRow[][] {
  if (count <= 1) return [rows];
  const blocks: MenuRow[][] = [];
  rows.forEach((row) => {
    const last = blocks[blocks.length - 1];
    if (row.kind === "entry" && last != null) last.push(row);
    else blocks.push([row]);
  });
  const target = Math.ceil(rows.length / count);
  const columns: MenuRow[][] = [];
  let current: MenuRow[] = [];
  blocks.forEach((block, index) => {
    // A new column, if this block would exceed the target and enough columns are left for the remaining blocks.
    if (
      current.length > 0 &&
      current.length + block.length > target &&
      columns.length < count - 1
    ) {
      columns.push(current);
      current = [];
    }
    current.push(...block);
    if (index === blocks.length - 1) columns.push(current);
  });
  return columns;
}

/** The path the WebView opens for [url] of a menu entry: the server's urls have no leading slash. */
export function menuPath(url: string): string {
  return /^[a-z][a-z0-9+.-]*:/i.test(url) || url.startsWith("/")
    ? url
    : `/${url}`;
}
