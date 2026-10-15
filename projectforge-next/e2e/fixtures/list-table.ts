import { expect, type Locator, type Page } from "@playwright/test";

/**
 * The rows of a list, and how to wait for them — for every spec that reads a cell.
 *
 * `tbody tr` is *not* that wait: while the first page is loading, `DataTable` renders eight rows of
 * `Skeleton` placeholders, and they are `tr`s in the same `tbody`. A spec that waits for one of them
 * and then reads a cell reads an empty box — and the read succeeds, because the cell is there. So does
 * the empty state, which is a single row spanning every column.
 *
 * The rows of the result set are the ones carrying `data-row-id` (see DataTableRow, which sets it from
 * the row's id for `useHighlightedRow`). Neither the skeletons nor the empty state have it, so it is
 * the one signal that says "these cells hold values of the database".
 */
export function listRows(scope: Page | Locator): Locator {
  return scope.locator("tbody tr[data-row-id]");
}

/**
 * Waits until the list shows real rows, and answers them.
 *
 * @param scope The page, or the part of it holding the table — a dialog, for a table that is a picker.
 * @param timeout How long to wait. Generous by default: this runs against the live system, and a first
 *   list call of a session pays for the route being compiled and the caches being filled.
 */
export async function waitForRows(
  scope: Page | Locator,
  timeout = 20_000
): Promise<Locator> {
  const rows = listRows(scope);
  await expect(rows.first()).toBeVisible({ timeout });
  return rows;
}

/**
 * The cells of the row a search narrowed the list to — the row holding `text`, waited for the same way.
 *
 * For a spec asserting on its own seeded entity: filling the search box leaves the table showing the
 * previous result set while the next one is fetched (`keepPreviousData`), so "a row is there" is true
 * before the row in question is.
 */
export async function waitForRow(
  scope: Page | Locator,
  text: string,
  timeout = 20_000
): Promise<Locator> {
  const row = listRows(scope).filter({ hasText: text }).first();
  await expect(row).toBeVisible({ timeout });
  return row;
}
