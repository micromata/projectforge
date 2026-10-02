import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { waitForRows } from "./fixtures/list-table";
import { KONTO_METADATA } from "../lib/metadata/konto.generated";
import { ACCOUNT_PAGE } from "../components/features/account/account.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The account page against the live backend (see ACCOUNT_PAGE and KontoEntityRest).
 *
 * Read-only: the database is a copy of production, and `KontoDO` supports no real delete, so an
 * inserted account number would stay occupied for good. Every save below is intercepted before it
 * reaches the server; what is asserted is what the page shows of the stored accounts and what the
 * form refuses on its own.
 */
test.describe("account", { tag: "@parallel" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/account/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("shows the declared columns under the labels of KontoDO", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/account");

    await expect(
      page.getByRole("heading", { name: t(ACCOUNT_PAGE.titleKey) })
    ).toBeVisible();
    for (const column of ACCOUNT_PAGE.columns) {
      const key = columnHeaderKeyOf(column, KONTO_METADATA);
      await expect(
        page.getByRole("columnheader", { name: t(key) }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("sorts by the account number by default, as Wicket's list does", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/account");
    const rows = await waitForRows(page);

    await expect
      .poll(
        async () => {
          const shown = (await rows.locator("td:first-child").allInnerTexts())
            .map((text) => Number(text.trim()))
            .filter((n) => !Number.isNaN(n));
          return (
            shown.length > 0 &&
            shown.every((n, i) => i === 0 || shown[i - 1] <= n)
          );
        },
        { message: "the number column must sort ascending" }
      )
      .toBe(true);
  });

  test("offers the seller's bank accounts as a select, not as free text", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const configured = (await (
      await page.request.get("/rs/account/sellerBankAccounts", {
        headers: { "X-PF-Frontend": "next" },
      })
    ).json()) as unknown[];
    await goto(page, "/account/new");

    const label = t("fibu.konto.sellerBankAccountName");
    // Where the installation configured bank accounts, the field is a choice among them (Wicket's
    // dropdown); only without any is it the text box keeping a stored value editable.
    if (configured.length > 0) {
      await expect(page.getByRole("combobox", { name: label })).toBeVisible();
      await expect(page.getByRole("textbox", { name: label })).toHaveCount(0);
    } else {
      await expect(page.getByRole("textbox", { name: label })).toBeVisible();
    }
  });

  test("refuses to save an account without number and name", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    let saveAttempted = false;
    await page.route("**/rs/account/saveorupdate*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    await goto(page, "/account/new");
    await page.getByRole("button", { name: t("save") }).click();

    for (const key of ["fibu.konto.nummer", "fibu.konto.bezeichnung"]) {
      await expect(
        page.getByText(t("validation.error.fieldRequired", { arg0: t(key) }))
      ).toHaveCount(1);
    }
    expect(
      saveAttempted,
      "an incomplete account must not reach the server"
    ).toBe(false);
  });
});
