import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { purgeTestAttachments } from "./fixtures/attachments";
import { listRows } from "./fixtures/list-table";
import { createBook, type SeededBook } from "./fixtures/seed";

/**
 * The attachments column and the "has attachments" filter of the books list, against the live
 * backend.
 *
 * No book can be assumed to have an attachment, so the column can't be asserted on a row's content —
 * the test uploads one onto a book of its own (`createBook`), checks what the list shows for exactly
 * that row, and deletes it again. The file name carries the `pf-e2e-` prefix so a leftover is
 * recognizable, and `purgeTestAttachments` removes one before the count is pinned.
 *
 * The filter's own assertion is on the request body: the backend turns `hasAttachments` into a
 * predicate on `attachmentsCounter` (AttachmentsFilterSupport), and a value sent in the wrong shape
 * would be dropped silently — the list would look fine and simply not filter.
 */
const FILE_NAME = "pf-e2e-column.txt";

test.describe("books list attachments", () => {
  let book: SeededBook;

  test.beforeAll(async ({ seedRequest }) => {
    book = await createBook(seedRequest);
  });

  // Before each, not only at the end: the filter is stored per user and per entity, so a
  // `hasAttachments` criterion left behind by an earlier (or aborted) run would hide the very book
  // the first case uploads to — the column would then be asserted on a list the filter emptied.
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
    // A file an aborted run left on the book would be counted by the assertion below, which pins the
    // count exactly ("(1)").
    await purgeTestAttachments(page, "book", book.id);
  });

  test("shows count and total size of a book's attachments", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);

    const downloadLink = page.getByRole("link", {
      name: `${t("download._")}: ${FILE_NAME}`,
    });

    await goto(page, `/book/${book.id}`);
    await page.getByLabel(t("file.upload.choose")).setInputFiles({
      name: FILE_NAME,
      mimeType: "text/plain",
      buffer: Buffer.from("ProjectForge e2e attachment\n"),
    });
    await expect(downloadLink).toBeVisible();

    try {
      // Straight to the book through the search box: the column is only filled in its row, and the
      // title is unique per run (see fixtures/seed.ts), so the list holds this one book.
      await goto(page, "/book");
      await page.getByPlaceholder(t("filter.searchList")).fill(book.signature);

      // Inside the seeded book's own row, not the first one that has a summary: the filtered answer
      // may not have arrived yet, and until it does the rows on screen are still the unfiltered
      // list's — an attachment on any of them would satisfy `.first()`.
      const row = listRows(page).filter({ hasText: book.signature });
      await expect(row).toHaveCount(1, { timeout: 30_000 });
      // The backend formats size and count together ("28bytes (1)") in the user's locale, so the
      // assertion pins the count and the unit rather than a hand-built string.
      const summary = row.getByRole("img", { name: t("attachments._") });
      await expect(summary).toContainText("(1)");
      await expect(summary).toContainText(/bytes|KB/);
    } finally {
      await goto(page, `/book/${book.id}`);
      await page
        .getByRole("button", { name: `${t("delete")}: ${FILE_NAME}` })
        .click();
      // The confirmation's own button carries the bare label, unlike the row's — and is looked up
      // inside the dialog rather than as the last one on the page: until the dialog has mounted,
      // "the last delete button" is the row's own, and clicking that reopens the question instead of
      // answering it. The file then stays, and the next run fails on a count one too high.
      await page
        .getByRole("alertdialog")
        .getByRole("button", { name: t("delete"), exact: true })
        .click();
      await expect(downloadLink).toHaveCount(0);
    }
  });

  test("filters the list by whether a book has attachments", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");

    await page.getByRole("button", { name: t("filter.addField") }).click();
    await page
      .getByRole("option", { name: t("attachments._"), exact: true })
      .click();

    // The pill popover applies live and has no save button (see filter-pill-shell): picking a value
    // fires the list request by itself, so the wait is armed before the click and matched on the body
    // it carries — not on a stray earlier request from opening the empty field. A LIST field's choices
    // lie open in the popover ([FilterListField]'s inline mode) rather than behind a select of their own.
    const request = page.waitForRequest(
      (candidate) =>
        candidate.url().includes("/rs/book/list") &&
        candidate.method() === "POST" &&
        (candidate.postData() ?? "").includes("hasAttachments")
    );
    await page
      .locator('[data-slot="popover-content"]')
      .getByRole("option", { name: t("yes"), exact: true })
      .click();

    const body = JSON.parse((await request).postData() ?? "{}") as {
      entries: { field: string; value: { values?: string[] } }[];
    };
    const entry = body.entries.find((e) => e.field === "hasAttachments");
    // MagicFilterProcessor reads `value.values`; the enum name is what the backend matches on.
    expect(entry?.value.values).toEqual(["YES"]);
  });

  // The filter is stored per user and per entity, so it must not leak into the other books specs.
  test.afterAll(async ({ request }) => {
    await request
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });
});
