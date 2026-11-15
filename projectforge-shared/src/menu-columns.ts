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

/**
 * [items] in their order over at most [count] columns, read top to bottom and then left to right, the tallest column as
 * low as possible. An item is never split; no column is empty. Used for the categories of the main menu (app and next):
 * unlike placing the largest items first into the shortest column, the menu order stays and a small change of the menu
 * (another right, another entry) doesn't rearrange all columns.
 */
export function balanceColumns<T>(
  items: T[],
  count: number,
  height: (item: T) => number,
): T[][] {
  if (items.length === 0) return [];
  if (count <= 1 || items.length === 1) return [items];
  const heights = items.map(height);
  // The smallest limit for which the items fit in order into [count] columns (heights are row counts, so integers).
  let low = Math.max(...heights);
  let high = heights.reduce((sum, it) => sum + it, 0);
  while (low < high) {
    const limit = Math.floor((low + high) / 2);
    if (columnsNeeded(heights, limit) <= count) high = limit;
    else low = limit + 1;
  }
  const columnCount = Math.min(count, items.length);
  const columns: T[][] = [];
  let current: T[] = [];
  let currentHeight = 0;
  items.forEach((item, index) => {
    const remainingItems = items.length - index;
    const remainingColumns = columnCount - columns.length - 1;
    // A new column, if this item would exceed the limit, or if every remaining column needs one of the remaining items.
    if (
      current.length > 0 &&
      (currentHeight + heights[index] > low ||
        remainingItems === remainingColumns)
    ) {
      columns.push(current);
      current = [];
      currentHeight = 0;
    }
    current.push(item);
    currentHeight += heights[index];
  });
  columns.push(current);
  return columns;
}

/** The number of columns the [heights] need in order, if no column exceeds [limit] (at least the largest height). */
function columnsNeeded(heights: number[], limit: number): number {
  let columns = 1;
  let current = 0;
  heights.forEach((it) => {
    if (current + it > limit) {
      columns++;
      current = 0;
    }
    current += it;
  });
  return columns;
}
