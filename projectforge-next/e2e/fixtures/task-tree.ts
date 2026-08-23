import { expect, type Page } from "@playwright/test";
import type { UserFormat } from "./format";
import { listRows } from "./list-table";
import { DEFAULT_PAGE_SIZE } from "../../components/data-table/page-size-options";

/**
 * The moves every structure-tree spec makes: put the account's tree state back to a known one, and
 * find the seeded task's row in a tree of thousands.
 *
 * Here rather than in a single spec because the tree keeps both of its states outside the client — the
 * filter in the session, the column layout in the user's prefs — so every spec that touches it has to
 * begin by neutralizing what a previous run (or the Wicket page, which shares the session filter) left
 * behind.
 */

/**
 * Drops the two states the tree stores server-side, so a case starts from the defaults.
 *
 * The filter is session-scoped and a search string left in it would empty the tree under test; a
 * non-initial call is what sets it, which is the same request the panel sends when the filter changes.
 * The column state lives in the account's prefs and outlives the browser context, so a column hidden
 * by another run must not decide what this one sees.
 *
 * Both modes, and that is the point of the loop: the backend keeps a filter and a grid state *per
 * mode* — the tree page under one key, the select popover of a task field under another
 * (`TaskServicesRest.filterKeySuffix` / `gridCategory`). Resetting only the page's left the popover
 * filtered by whatever the last case searched for, and since the popover then opens with that string
 * already in its field, typing the same one produces no request at all — which is what
 * [narrowToSeeded] waits for.
 *
 * Failures are swallowed: this is setup, and a spec that then finds no rows fails with a message about
 * the tree rather than about a preference call.
 */
export async function resetTreeState(page: Page): Promise<void> {
  for (const select of ["", "&select=true"]) {
    await page.request
      .get(
        `/rs/task/tree?table=true${select}&searchString=&opened=true&notOpened=true&closed=false&deleted=false`
      )
      .catch(() => undefined);
    await page.request
      .get(`/rs/task/tree/resetGridState/?select=${select ? "true" : "false"}`)
      .catch(() => undefined);
  }
}

/**
 * Narrows the tree to the seeded task and answers its row together with the number of rows left.
 *
 * Necessary, not merely tidy: the tasks of every run stay in the database (see ./seed.ts), so the
 * root's children outgrow a page of the table, and the newest of them — this run's — lands on the last
 * one. A search asks the backend for the matching subtrees, which brings the row onto page one.
 *
 * The count comes from here rather than from the caller, and only after the *filtered* answer has
 * arrived: the search is debounced, so for a while the table still shows the unfiltered page — a count
 * taken then is a count of the wrong list, and a later "more rows than before" can never reach it.
 * Keyed on the response rather than on "the row count stopped changing", because between the
 * keystrokes and the answer the table sits still at the old number for longer than any poll interval.
 */
export async function narrowToSeeded(
  page: Page,
  t: UserFormat["t"],
  title: string
) {
  // The whole label, not merely a part of it: a task select field carries a search button of its own,
  // named after the field it belongs to (see TaskSearchPopover), and `getByLabel` matches substrings.
  const search = page.getByLabel(t("search._"), { exact: true });
  // Cleared first where it already holds the title: filling a field with the value it has is no change,
  // the query key stays the same and no request goes out — the wait below would then time out although
  // the tree is showing exactly what was asked for. `resetTreeState` keeps that out of the session, and
  // this keeps it out of a case that searches twice.
  if ((await search.inputValue()) === title) {
    await search.fill("");
  }
  const filtered = page.waitForResponse(
    (response) =>
      response.url().includes("/rs/task/tree?") &&
      response.url().includes(searchTerm(title)) &&
      response.status() === 200,
    { timeout: 20_000 }
  );
  await search.fill(title);
  const { nodes = [] } = (await (await filtered).json()) as {
    nodes?: unknown[];
  };
  const rows = listRows(page);
  // The answer says how many rows the table will have, so waiting for that number is waiting for the
  // rendering of *this* answer rather than for an arbitrary moment of quiet. Capped at a page, since
  // a wider result would be paginated — the seeded subtree is far below one page.
  await expect(rows).toHaveCount(Math.min(nodes.length, DEFAULT_PAGE_SIZE), {
    timeout: 20_000,
  });
  const row = rows.filter({ hasText: title }).first();
  await expect(row).toBeVisible();
  return { row, count: await rows.count() };
}

/**
 * The part of a title that is safe to look for in a request url.
 *
 * The last word, not the whole one: the client builds the query with `URLSearchParams`, which writes a
 * space as "+" rather than "%20" — matching the encoded title would never hit.
 */
export function searchTerm(title: string): string {
  return title.split(" ").at(-1) ?? title;
}
