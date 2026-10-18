import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { ADDRESS_CAMPAIGN_VALUE_METADATA } from "../lib/metadata/address-campaign-value.generated";
import { ADDRESS_CAMPAIGN_VALUE_PAGE } from "../components/features/address-campaign-value/address-campaign-value.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The address campaign values of the marketing plugin against the live backend (see
 * ADDRESS_CAMPAIGN_VALUE_PAGE and AddressCampaignValueEntityRest).
 *
 * Read-only: the database is a copy of production. Nothing is saved; the row of an address without a value is
 * served by the test itself, as which campaign has such rows depends on the data.
 */
test.describe("address campaign values", { tag: "@parallel" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/addressCampaignValue/filter/reset", {
        headers: { "X-PF-Frontend": "next" },
      })
      .catch(() => undefined);
  });

  test("shows the declared columns and the campaign filter", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    await goto(page, "/address-campaign-value");

    await expect(
      page.getByRole("heading", {
        name: t(ADDRESS_CAMPAIGN_VALUE_PAGE.titleKey),
      })
    ).toBeVisible();
    // A value is only created from the row of its address, never from a blank form
    // (AddressCampaignValueEntityRest.listInsertAccess).
    await expect(
      page.getByLabel(t("menu.addNewEntry"), { exact: true })
    ).toHaveCount(0);
    // The comparison columns only appear with a comparison campaign in the filter, which the reset clears.
    const columns = ADDRESS_CAMPAIGN_VALUE_PAGE.columns.filter(
      (column) => !column.revealedByFilter
    );
    for (const column of columns) {
      const key = columnHeaderKeyOf(column, ADDRESS_CAMPAIGN_VALUE_METADATA);
      await expect(
        // The accessible name goes on with the header's buttons ("… Filter …"); "Adresse" must not match
        // "Adressstatus" or "Adressänderung".
        page.getByRole("columnheader", {
          name: new RegExp(`^${escapeRegExp(label(format, key))}( |$)`),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
    await expect(
      page.getByText(label(format, "plugins.marketing.addressCampaign"), {
        exact: true,
      })
    ).not.toHaveCount(0);
  });

  test("opens the form for a new value from the row of an address without one", async ({
    loggedInPage: page,
  }) => {
    await page.route("**/rs/addressCampaignValue/list*", (route) =>
      route.fulfill({
        json: {
          resultSet: [
            {
              id: -987654,
              addressId: 987654,
              addressCampaign: { id: 4711, title: "e2e campaign" },
              name: "E2e-Synthetic",
              firstName: "Row",
            },
          ],
          totalSize: 1,
          statistics: { campaignId: 4711, campaignTitle: "e2e campaign" },
        },
      })
    );
    await page.route("**/rs/addressCampaignValue/newEntry*", (route) =>
      route.fulfill({ json: { addressCampaign: { id: 4711 } } })
    );
    await goto(page, "/address-campaign-value");

    await expect(page.getByText("e2e campaign")).toBeVisible();
    await page.getByRole("cell", { name: "E2e-Synthetic" }).click();
    await expect(page).toHaveURL(
      /\/address-campaign-value\/new\?campaignId=4711&addressId=987654$/
    );
  });
});

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
