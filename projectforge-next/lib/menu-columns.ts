import type { MenuItem } from "./rs/types";

/**
 * Vertical weight of a category inside a column: its heading plus one row per entry.
 * Used only to compare categories against each other, so the unit is "rows", not pixels.
 */
function categoryHeight(category: MenuItem): number {
  return 1 + (category.subMenu?.length ?? 0);
}

function columnHeight(column: MenuItem[]): number {
  return column.reduce((sum, category) => sum + categoryHeight(category), 0);
}

/**
 * Distributes the main menu categories over `columnCount` columns of roughly equal height.
 *
 * A plain CSS grid cannot do this: every grid row is as tall as its tallest cell, so one large
 * category (e.g. "Administration") tears open a row and leaves gaps next to it. Instead the
 * categories are placed greedily — largest first into the currently shortest column — which keeps
 * the panel compact no matter how many entries a user's permissions unlock.
 *
 * The first category ("Allgemein"/COMMON) stays pinned to the first column so the most used
 * entries keep their familiar place. Categories without entries are dropped: they would only
 * render a dangling heading.
 */
export function balanceMenuColumns(
  categories: MenuItem[],
  columnCount: number
): MenuItem[][] {
  const filled = categories.filter((category) => category.subMenu?.length);
  if (filled.length === 0) return [];
  if (columnCount <= 1) return [filled];

  const columns: MenuItem[][] = Array.from(
    { length: Math.min(columnCount, filled.length) },
    () => []
  );
  const [first, ...rest] = filled;
  columns[0].push(first);

  // Sort a copy: the incoming array belongs to the react-query cache. Ties keep backend order,
  // so the result is deterministic for a given menu.
  rest
    .map((category, index) => ({ category, index }))
    .sort(
      (a, b) =>
        categoryHeight(b.category) - categoryHeight(a.category) ||
        a.index - b.index
    )
    .forEach(({ category }) => {
      const shortest = columns.reduce((a, b) =>
        columnHeight(a) <= columnHeight(b) ? a : b
      );
      shortest.push(category);
    });

  // Restore backend order within each column so the size heuristic stays invisible to the user.
  const order = new Map(filled.map((category, index) => [category, index]));
  return columns.map((column) =>
    [...column].sort((a, b) => order.get(a)! - order.get(b)!)
  );
}
