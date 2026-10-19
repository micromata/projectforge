import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { ADDRESSBOOK_METADATA } from "../lib/metadata/addressbook.generated";
import { ADDRESS_BOOK_PAGE } from "../components/features/addressBook/addressBook.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The address books against the live backend (see ADDRESS_BOOK_PAGE and AddressbookEntityRest).
 *
 * The database is a copy of production: nothing is written. The save test intercepts the save request.
 */
test.describe("addressBook", { tag: "@lane-addressBook" }, () => {
  test("shows the declared columns under the labels of AddressbookDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/addressBook");

    await expect(
      page.getByRole("heading", { name: format.t(ADDRESS_BOOK_PAGE.titleKey) })
    ).toBeVisible();
    for (const column of ADDRESS_BOOK_PAGE.columns) {
      const key = columnHeaderKeyOf(column, ADDRESSBOOK_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: new RegExp(`^${escape(label(format, key))}`),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("opens an address book with its access lists", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/addressBook");
    const rows = page.locator(ROW);
    // Every database has the global address book.
    await expect(rows.first()).toBeVisible();
    await rows.first().getByRole("cell").first().click();
    await expect(page).toHaveURL(/\/addressBook\/\d+/);
    await expect(
      page.getByText(t("addressbook.fullAccess.users"), { exact: true })
    ).toBeVisible();
    await expect(
      page.getByText(t("addressbook.readonlyAccess.groups"), { exact: true })
    ).toBeVisible();
  });

  test("posts a new address book on save", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    let saveAttempted = false;
    await page.route("**/rs/addressBook/{insert,update}*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    await goto(page, "/addressBook/new");
    await page
      .getByRole("textbox", { name: label(format, "addressbook.title") })
      .fill("e2e address book");
    await page.getByRole("button", { name: format.t("create") }).click();

    await expect
      .poll(() => saveAttempted, { message: "the save reaches the backend" })
      .toBe(true);
  });

  test("redirects the old React list", async ({ loggedInPage: page }) => {
    await page.goto("/react/addressBook");
    await expect(page).toHaveURL(/\/next\/addressBook$/);
  });
});

/**
 * A row of the loaded list: the `data-row-id` tells it from the skeleton rows shown while the list loads
 * and from the row of the empty list (see DataTable), which have cells as well.
 */
const ROW = "tbody tr[data-row-id]";

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
