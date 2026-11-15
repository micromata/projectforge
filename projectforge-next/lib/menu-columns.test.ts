import { describe, expect, it } from "vitest";
import { balanceMenuColumns } from "./menu-columns";
import type { MenuItem } from "./rs/types";

const category = (title: string, entries: number): MenuItem => ({
  title,
  subMenu: Array.from({ length: entries }, (_, index) => ({
    title: `${title}${index}`,
  })),
});

const titles = (columns: MenuItem[][]) =>
  columns.map((column) => column.map((it) => it.title));

describe("balanceMenuColumns", () => {
  it("keeps the menu order and balances the columns", () => {
    const menu = [
      category("Common", 11),
      category("Project", 10),
      category("HR", 3),
      category("Fibu", 10),
      category("Cost", 5),
      category("Reporting", 5),
      category("Orga", 5),
      category("Admin", 16),
      category("Misc", 5),
    ];
    expect(titles(balanceMenuColumns(menu, 4))).toEqual([
      ["Common", "Project"],
      ["HR", "Fibu", "Cost"],
      ["Reporting", "Orga"],
      ["Admin", "Misc"],
    ]);
  });

  it("drops categories without entries", () => {
    expect(
      titles(balanceMenuColumns([category("A", 2), category("B", 0)], 3))
    ).toEqual([["A"]]);
    expect(balanceMenuColumns([category("B", 0)], 3)).toEqual([]);
  });
});
