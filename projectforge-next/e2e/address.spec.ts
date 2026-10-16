import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { ADDRESS_METADATA } from "../lib/metadata/address.generated";
import { ADDRESS_PAGE } from "../components/features/address/address.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The addresses against the live backend (see ADDRESS_PAGE, AddressEntityRest and AddressImportRest).
 *
 * The database is a copy of production: nothing is written. The save test intercepts the save request; the text
 * parser and the VCF import only read (an import lives in the session until it is cancelled).
 */
test.describe("address", { tag: "@lane-address" }, () => {
  test("shows the declared columns under the labels of AddressDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/address");

    await expect(
      page.getByRole("heading", { name: format.t(ADDRESS_PAGE.titleKey) })
    ).toBeVisible();
    for (const column of ADDRESS_PAGE.columns) {
      const key = columnHeaderKeyOf(column, ADDRESS_METADATA);
      await expect(
        page.getByRole("columnheader", {
          // Whole words: the one-letter header of the favorites ("F") is the start of other headers as well.
          name: new RegExp(`^${escape(label(format, key))}(\\s|$)`),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("opens the view of an address in a dialog, and its form from there", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const list = page.waitForResponse("**/rs/address/list*");
    await goto(page, "/address");
    await list;
    const rows = page.getByRole("row").filter({ has: page.getByRole("cell") });
    // The list starts with the user's favorites, which a test account may not have.
    await rows
      .first()
      .waitFor({ timeout: 5_000 })
      .catch(() => undefined);
    test.skip(
      (await rows.count()) === 0,
      "no address in the list of this account"
    );
    await rows.first().getByRole("cell").nth(1).click();
    // As in the React list, the view opens over the list, which stays where it was.
    const dialog = page.getByRole("dialog");
    await expect(dialog).toBeVisible();
    await expect(page).toHaveURL(/\/address$/);
    await dialog.getByRole("link", { name: t("edit") }).click();
    await expect(page).toHaveURL(/\/address\/\d+\/edit\?returnTo=\/address$/);
    await expect(
      page.getByText(t("address.heading.personalData")).first()
    ).toBeVisible();
  });

  test("takes the fields of a pasted signature over into the form", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/address/new");
    await page.getByText(label(format, "address.parseText.button")).click();
    await page
      .getByLabel(format.t("address.parseText.inputLabel"))
      .fill("Dr. Erika Mustermann\nExample GmbH\nerika.mustermann@example.org");
    await page
      .getByRole("button", { name: format.t("parse"), exact: true })
      .click();
    await page.getByRole("button", { name: format.t("apply") }).click();
    await expect(
      page.getByRole("textbox", { name: label(format, "name"), exact: true })
    ).toHaveValue("Mustermann");
  });

  test("posts a new address of an organization only on save", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let saveAttempted = false;
    await page.route("**/rs/address/saveorupdate*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    await goto(page, "/address/new");
    // No name: name, first name or organization will do (AddressSaveSupport.validate).
    await page
      .getByLabel(label(format, "organization"), { exact: true })
      .fill("e2e organization");
    // The form of address is mandatory (AddressDO).
    await page
      .getByRole("combobox", { name: label(format, "address.form") })
      .click();
    await page
      .getByRole("option", { name: format.t("address.form.unknown") })
      .click();
    await page.getByRole("button", { name: format.t("save") }).click();

    await expect
      .poll(() => saveAttempted, { message: "the save reaches the backend" })
      .toBe(true);
  });

  test("imports a vCard into the form of a new address", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/address/import");
    await page.locator('input[type="file"]').setInputFiles({
      name: "e2e.vcf",
      mimeType: "text/vcard",
      buffer: Buffer.from(
        "BEGIN:VCARD\nVERSION:3.0\nN:E2e-Import-Nobody;Ina;;;\nFN:Ina E2e-Import-Nobody\nEND:VCARD\n"
      ),
    });
    const row = page.getByRole("row").filter({ hasText: "E2e-Import-Nobody" });
    await expect(row).toBeVisible();
    await row.getByRole("cell").nth(1).click();
    await expect(page).toHaveURL(/\/address\/new\?importIndex=0$/);
    // The form opens with the comparison of the vCard ticked for taking over.
    await expect(page.getByText("E2e-Import-Nobody").first()).toBeVisible();

    await goto(page, "/address/import");
    await page.getByRole("button", { name: format.t("cancel") }).click();
    await expect(page.locator('input[type="file"]')).toHaveCount(1);
  });

  test("redirects the old React pages", async ({ loggedInPage: page }) => {
    // The redirects are Spring's (OrphanedLinkFilter); the Next dev server doesn't proxy /react.
    test.skip(
      process.env.E2E_DEV_SERVER === "1",
      "needs Spring, not the dev server"
    );
    await page.goto("/react/address");
    await expect(page).toHaveURL(/\/next\/address$/);
    await page.goto("/react/addressView/dynamic/1");
    await expect(page).toHaveURL(/\/next\/address\/1$/);
  });
});

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
