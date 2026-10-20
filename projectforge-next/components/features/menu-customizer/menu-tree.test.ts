import { describe, expect, it } from "vitest";
import type { MenuItem } from "@/lib/rs/types";
import {
  addGroup,
  applyDrop,
  cleanForSave,
  containsItem,
  insertItem,
  removeItem,
  renameGroup,
} from "./menu-tree";
import { FAVORITES, TEMPLATE } from "./types";

const entry = (id: string): MenuItem => ({ id, title: id, url: `next/${id}` });
const group = (id: string, ...entries: MenuItem[]): MenuItem => ({
  id,
  title: id,
  subMenu: entries,
});
const ids = (menu: MenuItem[]) =>
  menu.map((item) =>
    item.subMenu
      ? `${item.id}[${item.subMenu.map((s) => s.id).join(",")}]`
      : item.id
  );

describe("menu tree", () => {
  const menu = [entry("a"), group("g", entry("b"), entry("c")), entry("d")];

  it("finds entries at the top level and in groups", () => {
    expect(containsItem(menu, "a")).toBe(true);
    expect(containsItem(menu, "c")).toBe(true);
    expect(containsItem(menu, "x")).toBe(false);
  });

  it("adds an entry only once", () => {
    expect(ids(insertItem(menu, FAVORITES, entry("x"), 1))).toEqual([
      "a",
      "x",
      "g[b,c]",
      "d",
    ]);
    expect(insertItem(menu, FAVORITES, entry("c"))).toBe(menu);
    expect(ids(insertItem(menu, "g", entry("x")))).toEqual([
      "a",
      "g[b,c,x]",
      "d",
    ]);
  });

  it("removes entries and groups, renames groups", () => {
    expect(ids(removeItem(menu, "b", "g"))).toEqual(["a", "g[c]", "d"]);
    expect(ids(removeItem(menu, "g"))).toEqual(["a", "d"]);
    expect(renameGroup(menu, "g", "New")[1].title).toBe("New");
    expect(ids(addGroup(menu, "N", "n"))).toEqual(["a", "g[b,c]", "d", "n[]"]);
  });

  it("adds from the template where it is dropped, never twice", () => {
    const drag = {
      kind: "item" as const,
      item: entry("x"),
      container: TEMPLATE,
    };
    expect(
      ids(
        applyDrop(menu, drag, {
          kind: "item",
          item: entry("c"),
          container: "g",
        })
      )
    ).toEqual(["a", "g[b,x,c]", "d"]);
    expect(
      ids(applyDrop(menu, drag, { kind: "item", container: FAVORITES }))
    ).toEqual(["a", "g[b,c]", "d", "x"]);
    const dup = {
      kind: "item" as const,
      item: entry("b"),
      container: TEMPLATE,
    };
    expect(applyDrop(menu, dup, { kind: "item", container: FAVORITES })).toBe(
      menu
    );
  });

  it("reorders within a container, to its end when dropped onto the container", () => {
    const drag = {
      kind: "item" as const,
      item: entry("a"),
      container: FAVORITES,
    };
    expect(
      ids(
        applyDrop(menu, drag, {
          kind: "item",
          item: entry("d"),
          container: FAVORITES,
        })
      )
    ).toEqual(["g[b,c]", "d", "a"]);
    const inGroup = { kind: "item" as const, item: entry("b"), container: "g" };
    expect(
      ids(applyDrop(menu, inGroup, { kind: "item", container: "g" }))
    ).toEqual(["a", "g[c,b]", "d"]);
  });

  it("moves between containers", () => {
    const drag = {
      kind: "item" as const,
      item: entry("a"),
      container: FAVORITES,
    };
    expect(
      ids(
        applyDrop(menu, drag, {
          kind: "item",
          item: entry("b"),
          container: "g",
        })
      )
    ).toEqual(["g[a,b,c]", "d"]);
    const out = { kind: "item" as const, item: entry("c"), container: "g" };
    expect(
      ids(applyDrop(menu, out, { kind: "item", container: FAVORITES }))
    ).toEqual(["a", "g[b]", "d", "c"]);
  });

  it("keeps groups at the top level and nothing goes into the template", () => {
    const g = { kind: "group" as const, item: menu[1], container: FAVORITES };
    const other = [...menu, group("h")];
    expect(applyDrop(other, g, { kind: "item", container: "h" })).toBe(other);
    const drag = {
      kind: "item" as const,
      item: entry("a"),
      container: FAVORITES,
    };
    expect(applyDrop(menu, drag, { kind: "item", container: TEMPLATE })).toBe(
      menu
    );
    expect(applyDrop(menu, drag, undefined)).toBe(menu);
  });

  it("saves entries without subMenu and leaves empty groups out", () => {
    const saved = cleanForSave([
      { ...entry("a"), subMenu: undefined },
      group("g", entry("b")),
      group("empty"),
    ]);
    expect(saved).toEqual([
      entry("a"),
      { id: "g", title: "g", subMenu: [entry("b")] },
    ]);
    expect("subMenu" in saved[0]).toBe(false);
  });
});
