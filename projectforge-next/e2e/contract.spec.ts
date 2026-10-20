import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { CONTRACT_METADATA } from "../lib/metadata/contract.generated";
import { CONTRACT_PAGE } from "../components/features/contract/contract.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The contracts against the live backend (see CONTRACT_PAGE and ContractEntityRest).
 *
 * The database may be a copy of production: nothing is written. The save test intercepts the save request, and the
 * list filter of the account is left as it is.
 */
test.describe("contract", { tag: "@parallel" }, () => {
  test("shows the declared columns under the labels of ContractDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/contract");

    await expect(
      page.getByRole("heading", { name: format.t(CONTRACT_PAGE.titleKey) })
    ).toBeVisible();
    for (const column of CONTRACT_PAGE.columns) {
      if (column.hiddenByDefault) continue;
      const key = columnHeaderKeyOf(column, CONTRACT_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: new RegExp(`^${escape(label(format, key))}`),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("offers the configured contract types to the account", async ({
    loggedInPage: page,
  }) => {
    // The types are the configuration's (orga.contractTypes), possibly none; what is asserted is the answer's shape.
    const response = await page.request.get("/rs/contract/types", {
      headers: { "X-PF-Frontend": "next" },
    });
    expect(response.status()).toBe(200);
    const types = (await response.json()) as unknown[];
    expect(Array.isArray(types)).toBe(true);
    for (const type of types) {
      expect(type).toEqual({
        value: expect.any(String),
        label: expect.any(String),
      });
    }
  });

  test("posts a new contract on save", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    let saveAttempted = false;
    await page.route("**/rs/contract/{insert,update}*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    // Today is preset as the date by the backend (ContractEntityRest.newBaseDO); the title is mandatory. Both fields
    // complete from other contracts, so they are comboboxes (SuggestInput).
    await goto(page, "/contract/new");
    await page
      .getByRole("combobox", { name: label(format, "title") })
      .first()
      .fill("e2e contract");
    await page
      .getByRole("combobox", {
        name: label(format, "legalAffaires.contract.coContractorA"),
      })
      .first()
      .fill("e2e co-contractor");
    await page.getByRole("button", { name: format.t("create") }).click();

    await expect
      .poll(() => saveAttempted, { message: "the save reaches the backend" })
      .toBe(true);
  });

  test("redirects the old React pages", async ({ loggedInPage: page }) => {
    await page.goto("/react/contract");
    await expect(page).toHaveURL(/\/next\/contract$/);
    await page.goto("/react/contract/edit");
    await expect(page).toHaveURL(/\/next\/contract\/new$/);
  });
});

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
