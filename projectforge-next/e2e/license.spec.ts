import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { LICENSE_METADATA } from "../lib/metadata/license.generated";
import { LICENSE_PAGE } from "../components/features/license/license.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The license page of the licensemanagement plugin ("Licenses / Hardware") against the live backend (see
 * LICENSE_PAGE and LicenseEntityRest).
 *
 * Read-only: the database is a copy of production. Every save below is intercepted before it reaches
 * the server; what is asserted is what the page shows and what the form refuses on its own.
 */
test.describe("license", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/license/filter/reset", {
        headers: { "X-PF-Frontend": "next" },
      })
      .catch(() => undefined);
  });

  test("shows the declared columns under the labels of LicenseDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    await goto(page, "/license");

    await expect(
      page.getByRole("heading", { name: t(LICENSE_PAGE.titleKey) })
    ).toBeVisible();
    // The default account is in every group, the admin group included, so it sees the key column too.
    // Through `label`: a key with children of its own (`key`, `device`) holds its text under `_`.
    for (const column of LICENSE_PAGE.columns) {
      const key = columnHeaderKeyOf(column, LICENSE_METADATA);
      await expect(
        page.getByRole("columnheader", { name: label(format, key) }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("presets one license and refuses to save without product and version", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    let saveAttempted = false;
    await page.route("**/rs/license/saveorupdate*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    await goto(page, "/license/new");
    await expect(
      page.getByRole("textbox", {
        name: t("plugins.licensemanagement.numberOfLicenses"),
      })
    ).toHaveValue("1");
    // The files need the stored license, so a new one only says so.
    await expect(
      page.getByText(t("attachment.onlyAvailableAfterSave"))
    ).toBeVisible();
    await page.getByRole("button", { name: t("save") }).click();

    for (const key of [
      "plugins.licensemanagement.product",
      "plugins.licensemanagement.version",
    ]) {
      await expect(
        page.getByText(t("validation.error.fieldRequired", { arg0: t(key) }))
      ).toHaveCount(1);
    }
    expect(
      saveAttempted,
      "an incomplete license must not reach the server"
    ).toBe(false);
  });
});
