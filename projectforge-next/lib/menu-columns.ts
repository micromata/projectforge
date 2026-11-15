import { balanceColumns } from "@projectforge/shared/menu-columns";
import type { MenuItem } from "./rs/types";

/**
 * Vertical weight of a category inside a column: its heading plus one row per entry.
 * Used only to compare categories against each other, so the unit is "rows", not pixels.
 */
function categoryHeight(category: MenuItem): number {
  return 1 + (category.subMenu?.length ?? 0);
}

/**
 * Distributes the main menu categories over at most `columnCount` columns of roughly equal height.
 *
 * A plain CSS grid cannot do this: every grid row is as tall as its tallest cell, so one large
 * category (e.g. "Administration") tears open a row and leaves gaps next to it. Instead the
 * categories keep the backend order, read top to bottom and then left to right, and the column
 * breaks are chosen so the tallest column is as low as possible (`balanceColumns`, shared with the
 * app's menu). The first category ("Allgemein"/COMMON) thereby stays on top of the first column, and
 * one more entry unlocked by a permission doesn't rearrange the whole panel.
 *
 * Categories without entries are dropped: they would only render a dangling heading.
 */
export function balanceMenuColumns(
  categories: MenuItem[],
  columnCount: number
): MenuItem[][] {
  const filled = categories.filter((category) => category.subMenu?.length);
  return balanceColumns(filled, columnCount, categoryHeight);
}
