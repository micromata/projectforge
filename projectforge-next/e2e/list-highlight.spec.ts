import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { createBook, MARKER, type SeededBook } from "./fixtures/seed";
import type { ResultSet } from "../lib/rs/types";

/**
 * Returning from an edit page marks the entry that was saved and brings it into view.
 *
 * The marked id is the backend's: `AbstractEntityRest.onAfterEdit` remembers it per user and category,
 * and every list call hands it back as `ResultSet.highlightRowId`. So the test saves a book rather
 * than faking a state, and asserts that the id the *response* names is the one that was edited —
 * that the pref travels is half the feature.
 *
 * The second case is why this is worth an e2e test at all: paging happens on the client over the
 * whole result set and the page index is remembered nowhere, so the list opens on page one and the
 * entry is usually not on it. A spec asserting only "the row is visible" would pass on a database
 * where it happened to be.
 *
 * The book is the test's own (see fixtures/seed.ts) — the local database is a copy of production, so
 * no spec may name a row of it. Written to the database: one book, and one edit of its comment per
 * case.
 */
test.describe("list highlight", () => {
  let book: SeededBook;

  test.beforeAll(async ({ seedRequest }) => {
    book = await createBook(seedRequest);
  });

  // A criterion left behind by another run would hide the book, and only a row the list holds can be
  // marked. Resets what the server stores for this account, as the other list specs do.
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  /**
   * Edits the seeded book and saves it, then answers with the list's response — the rows in the order
   * the table pages through them (sorting is the server's, `manualSorting: true`), and the id it asks
   * to highlight.
   *
   * The comment has to actually change: the save button is disabled while the form is clean (see
   * EntityEditActions), so a no-op save would never leave the page. The comment is the harmless
   * field for it — nothing else asserts on it, and the value stays inside the tests' own marker.
   */
  async function saveAndReturnToList(
    page: Parameters<typeof userFormat>[0]
  ): Promise<ResultSet<{ id: number }>> {
    const format = await userFormat(page);
    const { t } = format;
    await goto(page, `/book/${book.id}`);
    await expect(page.getByRole("textbox", { name: /titel/i })).toHaveValue(
      book.title
    );
    await page
      .getByRole("textbox", { name: label(format, "comment"), exact: true })
      .fill(`${MARKER} edited ${Date.now()}`);

    const listResponse = page.waitForResponse(
      (response) =>
        response.url().includes("/rs/book/list") &&
        response.request().method() === "POST" &&
        response.status() === 200,
      { timeout: 30_000 }
    );
    await page.getByRole("button", { name: t("save") }).click();
    await expect(page).toHaveURL(/\/book\/?$/, { timeout: 30_000 });
    return (await (await listResponse).json()) as ResultSet<{ id: number }>;
  }

  test("marks the row that was edited last and scrolls to it", async ({
    loggedInPage: page,
  }) => {
    test.setTimeout(90_000);
    const result = await saveAndReturnToList(page);

    expect(
      result.highlightRowId,
      "the list response must name the saved entry"
    ).toBe(book.id);

    const row = page.locator(`tbody tr[data-row-id="${book.id}"]`);
    await expect(row).toHaveClass(/row-highlighted/, { timeout: 30_000 });
    // The scroll, and the only assertion that can see it: the row is inside the table's scroll port
    // rather than merely in the document.
    await expect(row).toBeInViewport({ timeout: 30_000 });
  });

  test("pages to the row when it is not on the first page", async ({
    loggedInPage: page,
  }) => {
    test.setTimeout(90_000);
    const result = await saveAndReturnToList(page);

    const index = result.resultSet.findIndex((row) => row.id === book.id);
    expect(index, "the seeded book must be in the list").toBeGreaterThanOrEqual(
      0
    );
    // The size the table pages by, read off the pagination bar rather than assumed: it is stored per
    // user, so the default is not necessarily what this account sees.
    const pageSize = Number(
      await page.locator("select").last().inputValue({ timeout: 30_000 })
    );
    expect(pageSize, "the pagination must offer a size").toBeGreaterThan(0);
    const expectedPage = Math.floor(index / pageSize) + 1;

    // Skipped rather than failed when the row happens to sit on page one: on a fresh database that is
    // the only possible outcome, and there is then no jump to observe.
    test.skip(
      expectedPage === 1,
      "the seeded book is on the first page of this database"
    );

    // The active page button, told apart by the style the pagination gives it.
    await expect(
      page.getByRole("button", { name: String(expectedPage), exact: true })
    ).toHaveClass(/bg-primary/, { timeout: 30_000 });
    await expect(
      page.locator(`tbody tr[data-row-id="${book.id}"]`)
    ).toBeInViewport({ timeout: 30_000 });
  });
});
