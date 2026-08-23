import type { APIRequestContext } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import {
  createBook,
  fetchEntity,
  markAsDeleted,
  type SeededBook,
} from "./fixtures/seed";

/**
 * What the edit page of a *deleted* entry offers — the shared machinery of every hand-built page
 * (EntityEditPage, entityAccess), with a book as the entity that carries it.
 *
 * Two holes are guarded here, and the second is the one that cost data:
 *
 * - the page used to offer "mark as deleted" a second time and no way back, while
 *   `LayoutUtils.processEditPage` swaps the button for `undelete`;
 * - **every** write brought the entry back to life. A hand-built form posts its values *as* the DTO,
 *   `deleted` is in no schema, and `CandHMaster.copyValues` copies the property of the posted object
 *   onto the stored row — so a save, and just as much a book's lend-out (`BookServicesRest` hands the
 *   posted DTO to `saveOrUpdate`), silently undeleted the entry. Closed twice over: the form posts
 *   `deleted: true` for such an entry, and the page leaves it no control to write with at all.
 *
 * A book of its own, deleted before each case and left deleted afterwards: the other book specs share
 * `seededBook` and need it alive, and nothing inserted can be removed again (see fixtures/seed.ts).
 */

async function isDeleted(
  request: APIRequestContext,
  id: number
): Promise<boolean> {
  const stored = await fetchEntity<{ deleted?: boolean }>(request, "book", id);
  return stored.deleted === true;
}

test.describe("a deleted entry", () => {
  // Serial: every case works on the same book and puts it back into the deleted state it expects.
  test.describe.configure({ mode: "serial" });

  let book: SeededBook;
  let format: UserFormat;

  test.beforeAll(async ({ seedRequest }) => {
    book = await createBook(seedRequest);
  });

  test.beforeEach(async ({ loggedInPage: page, seedRequest }) => {
    format = await userFormat(page);
    if (!(await isDeleted(seedRequest, book.id))) {
      await markAsDeleted(seedRequest, "book", book.id);
    }
  });

  test.afterAll(async ({ seedRequest }) => {
    // Nothing active is left behind: the book stays in the database (it cannot be removed) but stays
    // deleted, which is what keeps it out of every list's default filter.
    if (!(await isDeleted(seedRequest, book.id))) {
      await markAsDeleted(seedRequest, "book", book.id);
    }
  });

  test("offers the restore in place of save and delete", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/book/${book.id}`);
    // The loaded entry, so the assertions below can't pass on a form that isn't there yet. The label is
    // matched at its start: a required field carries the asterisk FieldShell appends.
    await expect(
      page.getByRole("textbox", {
        name: new RegExp(`^${format.t("book.title._")}`, "i"),
      })
    ).toHaveValue(book.title);

    // Said outright, not left to be inferred from the button: the state of the entry is what explains
    // both the missing save and the read-only fields.
    await expect(
      page.getByText(format.t("entityEdit.deletedInfo"))
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: format.t("undelete") })
    ).toBeVisible();
    // Neither of the two the page shows for a live entry: a deleted one is read-only until it is
    // restored, and asking to delete it again says nothing.
    await expect(
      page.getByRole("button", { name: format.t("save") })
    ).toHaveCount(0);
    await expect(
      page.getByRole("button", { name: format.t("markAsDeleted") })
    ).toHaveCount(0);
  });

  test("brings the entry back and returns to the list", async ({
    loggedInPage: page,
    seedRequest,
  }) => {
    await goto(page, `/book/${book.id}`);
    await page.getByRole("button", { name: format.t("undelete") }).click();

    // The way a delete leaves the page: the list is where the entry is among the others again.
    await expect(page).toHaveURL(/\/book$/);
    expect(await isDeleted(seedRequest, book.id)).toBe(false);
  });

  test("shows its fields read-only, down to the entity's own actions", async ({
    loggedInPage: page,
  }) => {
    await goto(page, `/book/${book.id}`);
    const title = page.getByRole("textbox", {
      name: new RegExp(`^${format.t("book.title._")}`, "i"),
    });
    await expect(title).toHaveValue(book.title);

    // Nothing typed here could be saved, so nothing may be typeable: the restore ignores the form, and
    // a page that accepts entries and then drops them says nothing about it.
    await expect(title).toBeDisabled();
    // The dropdowns too, and by their own state rather than only by the fieldset around them: a select
    // whose trigger is merely natively disabled still offered the ✕ that clears it (see useFormReadOnly).
    const dropdowns = page.locator('form [role="combobox"]');
    const count = await dropdowns.count();
    expect(count).toBeGreaterThan(0);
    for (let i = 0; i < count; i++) {
      await expect(dropdowns.nth(i)).toBeDisabled();
    }
    await expect(
      page.getByRole("button", { name: new RegExp(`^${format.t("reset")}:`) })
    ).toHaveCount(0);
    // The book's own write, which posts the loaded DTO and would otherwise have resurrected the entry
    // (`BookServicesRest.lendOut` hands it to `saveOrUpdate`) — it is inside the sections, so the same
    // fieldset covers it.
    await expect(
      page.getByRole("button", {
        name: new RegExp(`^${format.t("book.lendOut")}$`, "i"),
      })
    ).toBeDisabled();
  });
});
