import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { BOOK_METADATA } from "../lib/metadata/book.generated";
import { BOOK_PAGE } from "../components/features/book/book.page";
import type { SeededBook } from "./fixtures/seed";
import type { ResultSet } from "../lib/rs/types";

/**
 * The sort order a user set survives leaving the list and coming back.
 *
 * Worth its own spec because the sorting is stored in two places that can drift apart: the backend
 * keeps it (`AbstractPagesRest.setColumnStates`) and the list seeds itself from it, but the read is
 * cached for the whole session (`staleTime: Infinity`), so what a *second* mount starts from is the
 * cache entry — and that one used to hold the state as of the first page load. The column state is
 * therefore held in step with what is written (see useRememberColumnState); this asserts the effect.
 *
 * The sort order is also what the highlight depends on: it pages to the entry by its index within the
 * order the server sent, so a list that reopened in a different order puts the row on another page than
 * the one the user is looking at (see useHighlightedRow). That the row is marked and paged to is
 * list-highlight.spec.ts's; this spec covers the order it needs.
 *
 * Asserted through the request rather than the header's arrow: the sorting drives the server query
 * (`manualSorting: true`), so `MagicFilter.sortProperties` of the list call is what the list is
 * actually sorted by — an arrow could be shown while the rows came back in another order.
 *
 * Read-only apart from the marker the cancel writes: the stored state is reset first and again at the
 * end, so the account is left as found.
 */
test.describe("list sort persistence", () => {
  /** The column the test sorts by: a book's year, which every book carries and no other spec sorts. */
  const SORT_COLUMN = "yearOfPublishing";

  test.beforeEach(async ({ loggedInPage: page }) => {
    // Drops the stored filter *and* the grid state, so the order under test is the one this test sets
    // rather than one a previous run left behind.
    await page.request
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test.afterEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("keeps the sort order when the list is left and reopened", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    test.setTimeout(120_000);
    const format = await userFormat(page);
    const { t } = format;

    await goto(page, "/book");
    await expect(
      page.getByRole("heading", { name: t(BOOK_PAGE.titleKey) })
    ).toBeVisible({ timeout: 60_000 });

    // By the year: one click on the header cell, which is what sorts (see DataTable — the whole cell
    // carries the handler, not a button inside it).
    const sorted = await sortBy(page, format, SORT_COLUMN);
    // The direction is TanStack's to choose (a numeric column starts descending), so it is read off
    // the call rather than asserted — what this spec is about is that the same order comes back.
    expect(
      sorted.sortProperties,
      "the click must sort the list on the server"
    ).toEqual([
      { property: SORT_COLUMN, sortOrder: expect.stringMatching(/ENDING$/) },
    ]);

    // Away and back, through an edit page that writes nothing: cancel is the case the report names,
    // and it exercises the same remount as a save without adding a row.
    const returned = await cancelAndReturnToList(page, format, seededBook);

    expect(
      returned.sortProperties,
      "the reopened list must still be sorted the way it was left"
    ).toEqual(sorted.sortProperties);
  });
});

/**
 * Clicks a column header once and answers with the filter the resulting list call sent.
 *
 * The header is found by the label the metadata carries for the column, not by a string of its own —
 * the account's language decides it. `headerLabelKey` wins where a column declares one (the year's
 * column shows "Jahr", while the field is "Jahr der Veröffentlichung").
 */
async function sortBy(
  page: Parameters<typeof userFormat>[0],
  format: Awaited<ReturnType<typeof userFormat>>,
  columnName: string
) {
  const declared = BOOK_PAGE.columns.find(
    (column) => "name" in column && column.name === columnName
  );
  const labelKey =
    declared && "headerLabelKey" in declared && declared.headerLabelKey
      ? declared.headerLabelKey
      : BOOK_METADATA.fields[columnName as keyof typeof BOOK_METADATA.fields]
          .i18nKey!;
  const header = page.getByRole("columnheader", {
    name: new RegExp(`^${format.t(labelKey)}(\\s|$)`),
  });
  await expect(header).toHaveCount(1, { timeout: 30_000 });

  const call = listCall(page);
  await header.click();
  return await call;
}

/** The MagicFilter of the next list call — what the list is really sorted and filtered by. */
async function listCall(page: Parameters<typeof userFormat>[0]) {
  const response = await page.waitForResponse(
    (candidate) =>
      candidate.url().includes("/rs/book/list") &&
      candidate.request().method() === "POST" &&
      candidate.status() === 200,
    { timeout: 30_000 }
  );
  const sent = response.request().postDataJSON() as {
    sortProperties?: { property: string; sortOrder: string }[];
  };
  const body = (await response.json()) as ResultSet<{ id: number }>;
  return { ...body, sortProperties: sent.sortProperties ?? [] };
}

/**
 * Opens a book from the list and leaves it through cancel, then answers with the list call the return
 * triggered.
 *
 * Both moves are the user's own — a click on the row, a click on cancel — and that is what the case
 * needs: they are client-side navigations, so the app keeps its query cache across them, which is
 * where the state under test lives. Reaching the edit page by URL instead would reload the page, drop
 * the cache and read the state from the server again — a different code path, and one that cannot show
 * the bug.
 *
 * Cancel rather than save: it writes no entity (only the marked id, through `/rs/book/cancel`), so the
 * spec adds no row to the database while remounting the list exactly as a save would.
 */
async function cancelAndReturnToList(
  page: Parameters<typeof userFormat>[0],
  format: Awaited<ReturnType<typeof userFormat>>,
  book: SeededBook
) {
  // Searched for rather than looked for on the page the list happens to show: the database is a copy
  // of production, so the book sits on an arbitrary page of it. The signature is unique per run (see
  // fixtures/seed.ts), and the search leaves the sort order alone — it only returns to page one.
  await page
    .getByPlaceholder(format.t(BOOK_PAGE.searchPlaceholderKey))
    .fill(book.signature);
  const row = page.locator(`tbody tr[data-row-id="${book.id}"]`);
  await expect(row).toHaveCount(1, { timeout: 30_000 });
  await row.click();
  await expect(page.getByRole("textbox", { name: /titel/i })).toHaveValue(
    book.title,
    { timeout: 30_000 }
  );

  const call = listCall(page);
  await page.getByRole("button", { name: format.t("cancel") }).click();
  await expect(page).toHaveURL(/\/book\/?$/, { timeout: 30_000 });
  return await call;
}
