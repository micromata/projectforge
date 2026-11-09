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

import type { Menus } from "@projectforge/shared/rs/menu";

import {
  defaultTab,
  flattenMenuEntries,
  menuColumns,
  menuPath,
  MenuRow,
  menuRows,
  MenuTab,
  searchMenuEntries,
  tabBadge,
} from "../menuModel";
import { webTabs } from "../webTabs";

const labels = {
  mainMenu: "Main menu",
  favorites: "Favorites",
  myAccount: "My account",
  recent: "Recently used",
};

const menus: Menus = {
  mainMenu: {
    menuItems: [
      {
        key: "COMMON",
        title: "Common",
        subMenu: [
          {
            key: "COMMON.ADDRESS_LIST",
            title: "Addresses",
            url: "next/address",
          },
          { key: "COMMON.CALENDAR", title: "Calendar", url: "next/calendar" },
        ],
      },
      {
        key: "FIBU",
        title: "Fibu",
        badge: { counter: 3 },
        subMenu: [
          {
            key: "FIBU.KREDITOR",
            title: "Kreditoren-Adressbuch",
            url: "next/creditor",
          },
          {
            key: "FIBU.BUCHUNG",
            title: "Buchungskonten",
            url: "next/account",
            badge: { counter: 3 },
          },
        ],
      },
    ],
  },
  favoritesMenu: {
    menuItems: [
      { key: "ADDRESS_LIST", title: "Addresses", url: "next/address" },
      {
        key: "folder",
        title: "Reports",
        subMenu: [{ key: "REPORT", title: "Report", url: "next/report" }],
      },
    ],
  },
  myAccountMenu: {
    menuItems: [
      {
        key: "MY_MENU",
        title: "Kai Example",
        subMenu: [
          { key: "MY_ACCOUNT", title: "My account", url: "next/myAccount" },
          {
            key: "LOGOUT",
            title: "Logout",
            url: "rs/logout",
            type: "RESTCALL",
          },
        ],
      },
    ],
  },
  recentMenu: {
    menuItems: [{ key: "CALENDAR", title: "Calendar", url: "next/calendar" }],
  },
};

const titles = (rows: MenuRow[]) =>
  rows.map((it) =>
    it.kind === "entry"
      ? it.entry.title
      : it.kind === "heading" || it.kind === "group"
        ? `[${it.title}]`
        : it.kind,
  );

describe("flattenMenuEntries", () => {
  test("every page once, the main menu first, without the logout", () => {
    const entries = flattenMenuEntries(menus, labels);
    expect(entries.map((it) => `${it.category}/${it.title}`)).toEqual([
      "Common/Addresses",
      "Common/Calendar",
      "Fibu/Kreditoren-Adressbuch",
      "Fibu/Buchungskonten",
      "Reports/Report",
      "My account/My account",
    ]);
    expect(entries[0].menuKey).toBe("COMMON.ADDRESS_LIST");
  });
});

describe("searchMenuEntries", () => {
  test("the start of the title before a word start before anywhere", () => {
    const entries = flattenMenuEntries(menus, labels);
    expect(searchMenuEntries(entries, "buch").map((it) => it.title)).toEqual([
      "Buchungskonten",
      "Kreditoren-Adressbuch",
    ]);
    expect(searchMenuEntries(entries, "adress").map((it) => it.title)).toEqual([
      "Kreditoren-Adressbuch",
    ]);
  });

  test("the category finds its entries", () => {
    expect(
      searchMenuEntries(flattenMenuEntries(menus, labels), "FIBU"),
    ).toHaveLength(2);
  });
});

describe("menuRows", () => {
  const closed = {
    term: "",
    openGroups: new Set<string>(),
    tab: "favorites" as MenuTab,
  };

  test("the favorites with their folders", () => {
    expect(titles(menuRows(menus, labels, closed))).toEqual([
      "Addresses",
      "[Reports]",
      "Report",
    ]);
  });

  test("the entries opened last", () => {
    expect(
      titles(menuRows(menus, labels, { ...closed, tab: "recent" })),
    ).toEqual(["Calendar"]);
  });

  test("the closed categories of the main menu, an open one with its entries", () => {
    expect(titles(menuRows(menus, labels, { ...closed, tab: "main" }))).toEqual(
      ["[Common]", "[Fibu]"],
    );
    const rows = menuRows(menus, labels, {
      ...closed,
      tab: "main",
      openGroups: new Set(["FIBU"]),
    });
    expect(titles(rows)).toEqual([
      "[Common]",
      "[Fibu]",
      "Kreditoren-Adressbuch",
      "Buchungskonten",
    ]);
    expect(
      rows.find((it) => it.kind === "group" && it.key === "FIBU"),
    ).toMatchObject({ open: true, badgeCounter: 3 });
  });

  test("the account menu below the user name, without the logout, and the theme", () => {
    expect(
      titles(menuRows(menus, labels, { ...closed, tab: "account" })),
    ).toEqual(["[Kai Example]", "My account", "theme"]);
  });

  test("a tab without entries (recent ones of an older server) says so", () => {
    const { recentMenu: _, ...older } = menus;
    expect(
      titles(menuRows(older, labels, { ...closed, tab: "recent" })),
    ).toEqual(["empty"]);
  });

  test("searching shows the hits of all tabs, whichever is chosen", () => {
    expect(
      titles(menuRows(menus, labels, { ...closed, tab: "recent", term: "my" })),
    ).toEqual(["[My account]", "My account", "searchData"]);
  });

  test("the first tab: the favorites, the main menu without any", () => {
    expect(defaultTab(menus)).toBe("favorites");
    expect(defaultTab({ ...menus, favoritesMenu: { menuItems: [] } })).toBe(
      "main",
    );
  });

  test("the badge of a tab sums its categories (main menu) or its entries, in folders too, each page once", () => {
    expect(tabBadge(menus, "main")).toBe(3);
    const favorites: Menus = {
      ...menus,
      favoritesMenu: {
        menuItems: [
          {
            key: "BUCHUNG",
            title: "Buchungskonten",
            url: "next/account",
            badge: { counter: 3 },
          },
          {
            key: "folder",
            title: "Fibu",
            subMenu: [
              {
                key: "ORDER",
                title: "Orders",
                url: "next/order",
                badge: { counter: 2 },
              },
              {
                key: "BUCHUNG2",
                title: "Buchungskonten",
                url: "next/account",
                badge: { counter: 3 },
              },
            ],
          },
        ],
      },
    };
    expect(tabBadge(favorites, "favorites")).toBe(5);
    expect(tabBadge(menus, "recent")).toBe(0);
  });

  test("searching shows the hits by category and the search in all data", () => {
    expect(
      titles(menuRows(menus, labels, { ...closed, term: " cal " })),
    ).toEqual(["[Common]", "Calendar", "searchData"]);
    const none = menuRows(menus, labels, { ...closed, term: "xyz" });
    expect(titles(none)).toEqual(["noMatch", "searchData"]);
    expect(none[1]).toMatchObject({ term: "xyz" });
  });
});

describe("menuColumns", () => {
  const heading = (key: string): MenuRow => ({
    kind: "heading",
    key,
    title: key,
  });
  const entry = (key: string): MenuRow => ({
    kind: "entry",
    key,
    entry: { key, title: key, url: key, category: "c" },
  });
  const keys = (columns: MenuRow[][]) =>
    columns.map((column) => column.map((it) => it.key));
  const rows = [
    heading("A"),
    entry("a1"),
    entry("a2"),
    heading("B"),
    entry("b1"),
    heading("C"),
    entry("c1"),
    entry("c2"),
  ];

  test("one column: the rows as they are", () => {
    expect(menuColumns(rows, 1)).toEqual([rows]);
  });

  test("the blocks in order, a heading with its entries", () => {
    expect(keys(menuColumns(rows, 2))).toEqual([
      ["A", "a1", "a2"],
      ["B", "b1", "C", "c1", "c2"],
    ]);
    expect(keys(menuColumns(rows, 3))).toEqual([
      ["A", "a1", "a2"],
      ["B", "b1"],
      ["C", "c1", "c2"],
    ]);
  });

  test("no more columns than blocks, none empty", () => {
    expect(keys(menuColumns(rows, 4))).toEqual([
      ["A", "a1", "a2"],
      ["B", "b1"],
      ["C", "c1", "c2"],
    ]);
    expect(menuColumns([heading("A"), entry("a1")], 3)).toHaveLength(1);
    expect(menuColumns([], 3)).toEqual([]);
  });
});

describe("menuPath", () => {
  test("a path of the server gets its slash, another site stays", () => {
    expect(menuPath("next/address")).toBe("/next/address");
    expect(menuPath("/next/address")).toBe("/next/address");
    expect(menuPath("https://example.org/x")).toBe("https://example.org/x");
  });
});

describe("the menu of the pages tab", () => {
  beforeEach(() => webTabs.reset());

  test("shown at the start, toggled by the tab", () => {
    expect(webTabs.getMenu()).toBe(true);
    webTabs.toggleMenu();
    expect(webTabs.getMenu()).toBe(false);
    webTabs.toggleMenu();
    expect(webTabs.getMenu()).toBe(true);
  });

  test("a page asked for closes it, also before the tab exists", () => {
    webTabs.open("pages", "/next/address/edit/1");
    expect(webTabs.getMenu()).toBe(false);
    expect(webTabs.takePending("pages")).toBe("/next/address/edit/1");
  });

  test("the chat leaves it alone", () => {
    webTabs.open("chat", "/next/chat/1");
    expect(webTabs.getMenu()).toBe(true);
  });
});

describe("a page for the login switched to", () => {
  test("opened once its tabs are reset, at once if they are already", () => {
    webTabs.reset(10);
    const opened: string[] = [];
    webTabs.afterReset(11, () => opened.push("later"));
    webTabs.afterReset(10, () => opened.push("now"));
    expect(opened).toEqual(["now"]);
    webTabs.reset(11);
    expect(opened).toEqual(["now", "later"]);
    webTabs.reset(12);
    expect(opened).toEqual(["now", "later"]);
  });
});
