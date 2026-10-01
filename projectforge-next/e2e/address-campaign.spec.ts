import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { ADDRESS_CAMPAIGN_METADATA } from "../lib/metadata/address-campaign.generated";
import { ADDRESS_CAMPAIGN_PAGE } from "../components/features/address-campaign/address-campaign.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The address campaign page of the marketing plugin against the live backend (see ADDRESS_CAMPAIGN_PAGE
 * and AddressCampaignEntityRest).
 *
 * Read-only: the database is a copy of production. Every save below is intercepted before it reaches
 * the server; what is asserted is what the page shows and what the form refuses on its own.
 */
test.describe("address campaign", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/addressCampaign/filter/reset", {
        headers: { "X-PF-Frontend": "next" },
      })
      .catch(() => undefined);
  });

  test("shows the declared columns under the labels of AddressCampaignDO", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/address-campaign");

    await expect(
      page.getByRole("heading", { name: t(ADDRESS_CAMPAIGN_PAGE.titleKey) })
    ).toBeVisible();
    for (const column of ADDRESS_CAMPAIGN_PAGE.columns) {
      const key = columnHeaderKeyOf(column, ADDRESS_CAMPAIGN_METADATA);
      await expect(
        page.getByRole("columnheader", { name: t(key) }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("refuses to save a campaign without title and values", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    let saveAttempted = false;
    await page.route("**/rs/addressCampaign/saveorupdate*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    await goto(page, "/address-campaign/new");
    await page.getByRole("button", { name: t("save") }).click();

    for (const key of ["title", "values"]) {
      await expect(
        page.getByText(t("validation.error.fieldRequired", { arg0: t(key) }))
      ).toHaveCount(1);
    }
    expect(
      saveAttempted,
      "an incomplete campaign must not reach the server"
    ).toBe(false);
  });

  test("refuses values without a single value, as Wicket's format check does", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    let saveAttempted = false;
    await page.route("**/rs/addressCampaign/saveorupdate*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    await goto(page, "/address-campaign/new");
    await page.getByRole("textbox", { name: t("title") }).fill("e2e campaign");
    await page.getByRole("textbox", { name: t("values") }).fill(" ; ;");
    await page.getByRole("button", { name: t("save") }).click();

    await expect(
      page.getByText(
        t("plugins.marketing.addressCampaign.values.invalidFormat")
      )
    ).toHaveCount(1);
    expect(
      saveAttempted,
      "values without a single value must not reach the server"
    ).toBe(false);
  });
});
