import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";

/**
 * Lending a book out and returning it, against the live backend.
 *
 * Unlike book-edit.spec.ts these tests *write*: both endpoints run through `saveOrUpdate`
 * (BookServicesRest), so they persist the whole book. The test therefore returns the book again at
 * the end of each case — the loan is the only thing it changes, and returning clears it completely.
 *
 * Labels and the date are taken from the logged-in user's locale (see fixtures/format), never spelled
 * out here.
 *
 * Same demo book as book-edit.spec.ts.
 */
const BOOK_ID = 316163;

function escape(label: string): string {
  return label.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** Exact match, so "Ausleihen" can't also match "Ausleihnotiz". */
function exactly(label: string): RegExp {
  return new RegExp(`^${escape(label)}$`, "i");
}

/**
 * Anchored at the start only: a required field's label ends in the asterisk FieldShell adds, and an
 * optional one may carry a parenthesised hint, so neither is an exact match on the plain text.
 */
function startingWith(label: string): RegExp {
  return new RegExp(`^${escape(label)}`, "i");
}

test.describe("book lend out", () => {
  // Serial: all cases work on the same book, and each one leaves it free again.
  test.describe.configure({ mode: "serial" });

  let format: UserFormat;
  let lendOut: RegExp;
  let returnBook: RegExp;
  let title: RegExp;
  let note: RegExp;

  test.beforeEach(async ({ loggedInPage: page }) => {
    format = await userFormat(page);
    lendOut = exactly(format.t("book.lendOut"));
    returnBook = exactly(format.t("book.returnBook"));
    title = startingWith(format.t("book.title._"));
    note = startingWith(format.t("book.lendOutNote"));
  });

  test.afterEach(async ({ loggedInPage: page }) => {
    // Leave the book free, whatever the test did to it.
    await goto(page, `/book/${BOOK_ID}`);
    await expect(page.getByRole("textbox", { name: title })).toHaveValue(
      /Selenium/
    );
    const back = page.getByRole("button", { name: returnBook });
    if (await back.isVisible()) {
      await back.click();
      await expect(back).toBeHidden();
    }
  });

  test("lends out to the logged-in user and stays on the page", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/book/${BOOK_ID}`);
    await expect(page.getByRole("textbox", { name: title })).toHaveValue(
      /Selenium/
    );

    // A note that was never saved: it has to travel with the action, because the endpoint saves the
    // posted book. This is the assertion that proves the form values are what gets sent.
    const marker = `e2e ${Date.now()}`;
    await page.getByRole("textbox", { name: note }).fill(marker);

    await page.getByRole("button", { name: lendOut }).click();

    // The borrower is taken from the session server-side, so the line names the test account and
    // today's date, and the return button appears because it is this user's loan.
    const back = page.getByRole("button", { name: returnBook });
    await expect(back).toBeVisible();
    // A *date*, not a timestamp: lendOutDate is a LocalDate, which the legacy component rendered
    // with a meaningless 00:00.
    const today = format.date(new Date().toISOString().slice(0, 10));
    await expect(page.getByText(`, ${today}`, { exact: false })).toBeVisible();
    // Still on the edit page — the backend's REDIRECT to the list is ignored.
    await expect(page).toHaveURL(new RegExp(`/book/${BOOK_ID}$`));

    // Reload: the loan and the unsaved note were really persisted, not just put into local state.
    await page.reload();
    await expect(page.getByRole("button", { name: returnBook })).toBeVisible();
    await expect(page.getByRole("textbox", { name: note })).toHaveValue(marker);
  });

  test("returning clears the loan", async ({ loggedInPage: page }) => {
    await goto(page, `/book/${BOOK_ID}`);
    await page.getByRole("button", { name: lendOut }).click();
    const back = page.getByRole("button", { name: returnBook });
    await expect(back).toBeVisible();

    await back.click();

    // All three loan fields are cleared by returnBook, so the button disappears with the loan and
    // the note is empty again.
    await expect(back).toBeHidden();
    await expect(page.getByRole("textbox", { name: note })).toHaveValue("");
    await page.reload();
    await expect(page.getByRole("button", { name: returnBook })).toBeHidden();
  });

  test("offers no loan action before the first save", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/book/new");
    await expect(page.getByRole("textbox", { name: title })).toBeVisible();
    // Lending out writes the entity; there is nothing to write yet (legacy: `if (dto.id != null)`).
    await expect(page.getByRole("button", { name: lendOut })).toHaveCount(0);
    await expect(page.getByRole("button", { name: returnBook })).toHaveCount(0);
  });

  test("validates before lending out", async ({ loggedInPage: page }) => {
    await goto(page, `/book/${BOOK_ID}`);
    const titleField = page.getByRole("textbox", { name: title });
    await expect(titleField).toHaveValue(/Selenium/);
    await titleField.fill("");

    let posted = false;
    await page.route("**/rs/book/lendOut", (route) => {
      posted = true;
      return route.abort();
    });
    await page.getByRole("button", { name: lendOut }).click();

    // Same submit as saving, so the same Zod schema runs first — a book without a title never
    // reaches the server, which would otherwise save the emptied title along with the loan.
    expect(posted).toBe(false);
    await expect(page.getByRole("button", { name: returnBook })).toHaveCount(0);
  });
});
