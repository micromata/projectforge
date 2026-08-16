import { describe, expect, it } from "vitest";
import type { MenuData, MenuItem } from "./rs/types";
import {
  flattenMenuEntries,
  groupMenuEntries,
  searchMenuEntries,
  type MenuCategoryLabels,
} from "./menu-search";

const LABELS: MenuCategoryLabels = {
  mainMenu: "Main menu",
  favorites: "Favourites",
  myAccount: "My account",
};

/**
 * A leaf as the main menu sends it: its key qualified by the category, as MenuItemDef.id is. The
 * favourites and the account menu send the same entry with the bare name — [bareLeaf].
 */
function leaf(name: string, title: string, category = "COMMON"): MenuItem {
  return { key: `${category}.${name}`, title, url: `next/${name}` };
}

function bareLeaf(name: string, title: string): MenuItem {
  return { key: name, title, url: `next/${name}` };
}

function category(title: string, subMenu: MenuItem[]): MenuItem {
  return { key: title, title, subMenu };
}

/** A menu of the shape MenuRest sends: categories in the main menu, a flat favourites row. */
function menu(overrides: Partial<MenuData> = {}): MenuData {
  return {
    mainMenu: {
      menuItems: [
        category("Common", [
          leaf("ADDRESS_LIST", "Addresses"),
          leaf("ADDRESSBOOK_LIST", "Address books"),
        ]),
        category("Fibu", [
          leaf(
            "INCOMING_INVOICE_LIST",
            "Creditor invoices - addresses",
            "FIBU"
          ),
          leaf("ORDER_LIST", "Orderbook", "FIBU"),
        ]),
      ],
    },
    favoritesMenu: { menuItems: [] },
    myAccountMenu: { menuItems: [] },
    ...overrides,
  };
}

describe("flattenMenuEntries", () => {
  it("takes the leaves of every category, with the category as their group", () => {
    const entries = flattenMenuEntries(menu(), LABELS);
    expect(entries.map((entry) => [entry.key, entry.category])).toEqual([
      ["next/ADDRESS_LIST", "Common"],
      ["next/ADDRESSBOOK_LIST", "Common"],
      ["next/INCOMING_INVOICE_LIST", "Fibu"],
      ["next/ORDER_LIST", "Fibu"],
    ]);
  });

  it("passes MenuItem.key through as menuKey, for reporting the entry as used", () => {
    const entries = flattenMenuEntries(
      menu({
        favoritesMenu: { menuItems: [bareLeaf("VACATION", "Vacation")] },
      }),
      LABELS
    );
    // Unchanged, qualified or bare as the tree sent it: the server normalizes it, and the entry's
    // identity here stays the url.
    expect(entries[0]).toMatchObject({
      key: "next/ADDRESS_LIST",
      menuKey: "COMMON.ADDRESS_LIST",
    });
    expect(entries.at(-1)).toMatchObject({
      key: "next/VACATION",
      menuKey: "VACATION",
    });
  });

  it("answers with nothing while the menu is still loading", () => {
    expect(flattenMenuEntries(undefined, LABELS)).toEqual([]);
  });

  it("keeps a favourite once, under the category it belongs to", () => {
    const entries = flattenMenuEntries(
      menu({
        // The favourites row sends the bare key, the main menu the qualified one — the url is what
        // identifies the two rows as the same entry.
        favoritesMenu: { menuItems: [bareLeaf("ORDER_LIST", "Orderbook")] },
      }),
      LABELS
    );
    const orderbook = entries.filter(
      (entry) => entry.key === "next/ORDER_LIST"
    );
    expect(orderbook).toHaveLength(1);
    // The main menu is collected first, so its category wins over "Favourites".
    expect(orderbook[0].category).toBe("Fibu");
  });

  it("groups a favourite that is in no category under the favourites", () => {
    const entries = flattenMenuEntries(
      menu({
        favoritesMenu: { menuItems: [bareLeaf("VACATION", "Vacation")] },
      }),
      LABELS
    );
    expect(entries.at(-1)).toMatchObject({
      key: "next/VACATION",
      category: "Favourites",
    });
  });

  it("unwraps the account menu from below the user's name", () => {
    const entries = flattenMenuEntries(
      menu({
        myAccountMenu: {
          menuItems: [
            {
              key: "MY_MENU",
              title: "Kai Reinhard",
              subMenu: [bareLeaf("MY_ACCOUNT", "My account")],
            },
          ],
        },
      }),
      LABELS
    );
    expect(entries.at(-1)).toMatchObject({
      key: "next/MY_ACCOUNT",
      category: "My account",
    });
  });

  it("drops the logout, whose url is a rest call rather than a page", () => {
    const entries = flattenMenuEntries(
      menu({
        myAccountMenu: {
          menuItems: [
            {
              key: "MY_MENU",
              title: "Kai Reinhard",
              subMenu: [
                bareLeaf("MY_ACCOUNT", "My account"),
                {
                  key: "LOGOUT",
                  title: "Logout",
                  url: "logout",
                  type: "RESTCALL",
                },
              ],
            },
          ],
        },
      }),
      LABELS
    );
    expect(entries.map((entry) => entry.title)).not.toContain("Logout");
  });

  it("drops an entry without a url, which is no destination", () => {
    const entries = flattenMenuEntries(
      menu({
        favoritesMenu: { menuItems: [{ key: "EMPTY", title: "Nowhere" }] },
      }),
      LABELS
    );
    expect(entries.map((entry) => entry.title)).not.toContain("Nowhere");
  });

  it("takes a hand-built item without a key, whose url identifies it all the same", () => {
    const entries = flattenMenuEntries(
      menu({
        favoritesMenu: { menuItems: [{ title: "Somewhere", url: "wa/some" }] },
      }),
      LABELS
    );
    expect(entries.at(-1)?.key).toBe("wa/some");
  });

  it("carries the badge counter along", () => {
    const entries = flattenMenuEntries(
      menu({
        favoritesMenu: {
          menuItems: [
            { ...bareLeaf("VACATION", "Vacation"), badge: { counter: 3 } },
          ],
        },
      }),
      LABELS
    );
    expect(entries.at(-1)?.badgeCounter).toBe(3);
  });
});

describe("searchMenuEntries", () => {
  const entries = flattenMenuEntries(menu(), LABELS);

  it("keeps everything in menu order for an empty term", () => {
    expect(searchMenuEntries(entries, "   ")).toEqual(entries);
  });

  it("puts a hit at the start of the title first", () => {
    expect(
      searchMenuEntries(entries, "ad").map((entry) => entry.title)
    ).toEqual(["Addresses", "Address books", "Creditor invoices - addresses"]);
  });

  it("ranks a hit at a word start above one inside a word", () => {
    const titles = searchMenuEntries(entries, "book").map(
      (entry) => entry.title
    );
    // "Address books" has it at a word start, "Orderbook" only inside one.
    expect(titles).toEqual(["Address books", "Orderbook"]);
  });

  it("ignores case", () => {
    expect(searchMenuEntries(entries, "ORDERBOOK")).toHaveLength(1);
  });

  it("finds a whole category by its name, ranked last", () => {
    const found = searchMenuEntries(entries, "fibu");
    expect(found.map((entry) => entry.title)).toEqual([
      "Creditor invoices - addresses",
      "Orderbook",
    ]);
  });

  it("answers with nothing when the term matches no entry", () => {
    expect(searchMenuEntries(entries, "xyzzy")).toEqual([]);
  });

  it("treats a term with regex characters as text", () => {
    expect(searchMenuEntries(entries, "a.d")).toEqual([]);
  });
});

describe("groupMenuEntries", () => {
  it("groups by category, keeping the order of the entries", () => {
    const groups = groupMenuEntries(
      searchMenuEntries(flattenMenuEntries(menu(), LABELS), "ad")
    );
    expect(
      groups.map((group) => [group.category, group.entries.length])
    ).toEqual([
      ["Common", 2],
      ["Fibu", 1],
    ]);
  });
});
