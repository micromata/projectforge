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
    const rows = page.locator(ROW);
    // The list starts with the user's favorites, which a test account may not have.
    await rows
      .first()
      .waitFor({ timeout: 5_000 })
      .catch(() => undefined);
    test.skip(
      (await rows.count()) === 0,
      "no address in the list of this account"
    );
    // A cell after the star and the pencil (see the next test).
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

  test("opens the form from the pencil of a row, zooms a hovered phone number and calls it in a dialog", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const list = page.waitForResponse("**/rs/address/list*");
    await goto(page, "/address");
    await list;
    const rows = page.locator(ROW);
    await rows
      .first()
      .waitFor({ timeout: 5_000 })
      .catch(() => undefined);
    test.skip(
      (await rows.count()) === 0,
      "no address in the list of this account"
    );

    // A phone number, hovered, is shown large across the top of the window (as in the React list).
    const phone = page.getByRole("cell").locator("li a").first();
    if ((await phone.count()) > 0) {
      const number = (await phone.textContent())!.trim();
      await phone.hover();
      await expect(page.getByText(number, { exact: true })).toHaveCount(2);
      await page.mouse.move(0, 0);
      await expect(page.getByText(number, { exact: true })).toHaveCount(1);
    }

    // The direct call and the text message open in a dialog over the list (where configured), as in React.
    const call = page.locator('a[href*="/phoneCall?"]').first();
    if ((await call.count()) > 0) {
      await call.click();
      await expect(
        page
          .getByRole("dialog")
          .getByRole("heading", { name: t("address.phoneCall.title") })
      ).toBeVisible();
      await expect(page).toHaveURL(/\/address$/);
      await page.keyboard.press("Escape");
      await expect(page.getByRole("dialog")).toHaveCount(0);
    }
    const sms = page.locator('a[href*="/sendTextMessage?"]').first();
    if ((await sms.count()) > 0) {
      await sms.click();
      await expect(
        page
          .getByRole("dialog")
          .getByRole("heading", { name: t("address.sendSms.title") })
      ).toBeVisible();
      await expect(page).toHaveURL(/\/address$/);
      await page.keyboard.press("Escape");
      await expect(page.getByRole("dialog")).toHaveCount(0);
    }

    await rows
      .first()
      .getByRole("link", { name: t("edit") })
      .click();
    await expect(page).toHaveURL(/\/address\/\d+\/edit\?returnTo=\/address$/);
    await expect(page.getByRole("dialog")).toHaveCount(0);
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

  test("adds and removes the kinds of addresses in their one card", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/address/new");
    const business = t("address.heading.businessAddress");
    const postal = t("address.heading.postalAddress");
    const privateAddress = t("address.heading.privateAddress");
    const block = (heading: string) =>
      page.getByRole("group", { name: heading, exact: true });
    const add = page.getByRole("button", { name: t("address.addAddress") });

    // A new address starts with the business address; the picker offers the other two.
    await expect(block(business)).toBeVisible();
    await add.click();
    await expect(page.getByRole("menuitem")).toHaveText([
      postal,
      privateAddress,
    ]);
    await page.getByRole("menuitem", { name: privateAddress }).click();
    await expect(block(privateAddress)).toBeVisible();
    await add.click();
    await expect(page.getByRole("menuitem")).toHaveText([postal]);
    await page.keyboard.press("Escape");

    await block(privateAddress)
      .getByRole("button", {
        name: `${t("address.removeAddress")}: ${privateAddress}`,
      })
      .click();
    await expect(block(privateAddress)).toHaveCount(0);
    await expect(block(business)).toBeVisible();
  });

  test("posts a new address of an organization only on save", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let saveAttempted = false;
    await page.route("**/rs/address/{insert,update}*", (route) => {
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
    await page.getByRole("button", { name: format.t("create") }).click();

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
    // The form opens with the comparison of the vCard ticked for taking over, which is all the parser offers.
    await expect(page.getByText("E2e-Import-Nobody").first()).toBeVisible();
    await expect(
      page.getByText(format.t("address.book.vCardsImport.dataLoaded"))
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: format.t("apply") })
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: format.t("cancel"), exact: true })
    ).toHaveCount(1); // The form's own, not the parser's.

    // The columns of the import table are stored per user.
    const columnStates = page.waitForResponse(
      "**/rs/addressImport/columnStates"
    );
    await goto(page, "/address/import");
    expect((await columnStates).status()).toBe(200);
    await page.getByRole("button", { name: format.t("cancel") }).click();
    await expect(page.locator('input[type="file"]')).toHaveCount(1);
  });

  test("crops and scales a chosen image in the image editor before the upload", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/address/new");
    // A 400 × 200 image, drawn by the browser: no image file to keep beside the spec.
    const png = await page.evaluate(() => {
      const canvas = document.createElement("canvas");
      canvas.width = 400;
      canvas.height = 200;
      const context = canvas.getContext("2d")!;
      context.fillStyle = "steelblue";
      context.fillRect(0, 0, 400, 200);
      return canvas.toDataURL("image/png").split(",")[1];
    });
    await page.locator('input[type="file"]').setInputFiles({
      name: "e2e-image.png",
      mimeType: "image/png",
      buffer: Buffer.from(png, "base64"),
    });

    const dialog = page.getByRole("dialog", {
      name: format.t("imageEditor.title"),
    });
    await expect(dialog).toBeVisible();
    await dialog.getByRole("radio", { name: "1:1" }).click();
    await dialog.getByLabel(format.t("imageEditor.width")).fill("120");
    await expect(dialog.getByText("120 × 120 px")).toBeVisible();
    const upload = page.waitForResponse("**/rs/address/uploadImage/-1");
    await dialog.getByRole("button", { name: format.t("apply") }).click();
    expect((await upload).ok()).toBe(true);
    await expect(dialog).toBeHidden();

    // The preview is the uploaded result, square and scaled down. The form isn't saved: the upload stays in
    // the session until the next form discards it.
    const preview = page.getByRole("img", { name: format.t("address.image") });
    await expect
      .poll(() =>
        preview.evaluate((img: HTMLImageElement) =>
          img.complete ? `${img.naturalWidth}x${img.naturalHeight}` : ""
        )
      )
      .toBe("120x120");
  });

  test("refuses a file that is no image before the image editor opens", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/address/new");
    // `accept` only filters the file chooser; a drop (like setInputFiles) may hand over anything.
    await page.locator('input[type="file"]').setInputFiles({
      name: "e2e-not-an-image.txt",
      mimeType: "text/plain",
      buffer: Buffer.from("no image"),
    });
    await expect(
      page.locator("[data-sonner-toaster]").getByText(
        format.t("file.upload.error.unsupportedFormat", {
          arg0: "PNG, JPEG, GIF",
        })
      )
    ).toBeVisible();
    await expect(
      page.getByRole("dialog", { name: format.t("imageEditor.title") })
    ).toHaveCount(0);
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
