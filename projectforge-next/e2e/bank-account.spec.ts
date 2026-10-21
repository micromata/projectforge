import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { BANK_ACCOUNT_METADATA } from "../lib/metadata/bank-account.generated";
import { BANK_ACCOUNT_RECORD_METADATA } from "../lib/metadata/bank-account-record.generated";
import { BANK_ACCOUNT_PAGE } from "../components/features/bank-account/bank-account.page";
import { BANK_ACCOUNT_RECORD_PAGE } from "../components/features/bank-account-record/bank-account-record.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";
import type { Page } from "@playwright/test";

/**
 * The banking plugin against the live backend: bank accounts, their transactions and the import page (see
 * BANK_ACCOUNT_PAGE, BANK_ACCOUNT_RECORD_PAGE, BankAccountEntityRest, BankAccountRecordEntityRest).
 *
 * The database may be a copy of production: nothing is written. The save test intercepts the save request, and
 * no import is uploaded — a commit would mark the stored transactions of the file's period missing in the file as
 * deleted.
 */
test.describe("banking", { tag: "@parallel" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    // The plugin may be switched off in this installation.
    const response = await page.request.get("/rs/bankAccount/listMeta");
    test.skip(!response.ok(), "The banking plugin is not active.");
  });

  test("shows the declared columns of the bank accounts", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/bankAccount");
    await expectHeading(page, format.t(BANK_ACCOUNT_PAGE.titleKey));
    for (const column of BANK_ACCOUNT_PAGE.columns) {
      if (column.hiddenByDefault) continue;
      const key = columnHeaderKeyOf(column, BANK_ACCOUNT_METADATA);
      await expectColumn(page, label(format, key), columnIdOf(column));
    }
  });

  test("shows the declared columns of the transactions", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/bankAccountRecord");
    await expectHeading(page, format.t(BANK_ACCOUNT_RECORD_PAGE.titleKey));
    for (const column of BANK_ACCOUNT_RECORD_PAGE.columns) {
      if (column.hiddenByDefault) continue;
      const key = columnHeaderKeyOf(column, BANK_ACCOUNT_RECORD_METADATA);
      await expectColumn(page, label(format, key), columnIdOf(column));
    }
  });

  test("posts the account with its access lists on save", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let posted: Record<string, unknown> | undefined;
    await page.route("**/rs/bankAccount/{insert,update}*", (route) => {
      posted = route.request().postDataJSON() as Record<string, unknown>;
      return route.abort();
    });

    await goto(page, "/bankAccount/new");
    await page
      .getByRole("textbox", {
        name: label(format, "plugins.banking.account.name"),
      })
      .fill("e2e account");
    await page
      .getByRole("textbox", {
        name: label(format, "plugins.banking.account.bank"),
      })
      .fill("e2e bank");
    await page.getByRole("button", { name: format.t("create") }).click();

    await expect
      .poll(() => posted, { message: "the save reaches the backend" })
      .toBeDefined();
    const body = (posted?.data ?? posted) as Record<string, unknown>;
    expect(body.name).toBe("e2e account");
    expect(body.bank).toBe("e2e bank");
    // The logged-in user is preset with full access (BankAccountEntityRest.newBaseDTO); the minimal access lists
    // the form doesn't offer travel along, so a save doesn't empty them.
    expect((body.fullAccessUsers as unknown[]).length).toBe(1);
    expect(body.minimalAccessUsers).toEqual([]);
  });

  test("offers the import with the choice of the account", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/bankAccount/import");
    await expect(
      page.getByRole("heading", {
        name: format.t("plugins.banking.account.record.import.title"),
      })
    ).toBeVisible();
    await expect(
      page.getByText(label(format, "plugins.banking.account._"), {
        exact: true,
      })
    ).toBeVisible();
  });
});

async function expectHeading(page: Page, name: string): Promise<void> {
  await expect(page.getByRole("heading", { name })).toBeVisible();
}

async function expectColumn(
  page: Page,
  text: string,
  columnId: string
): Promise<void> {
  await expect(
    page.getByRole("columnheader", { name: new RegExp(`^${escape(text)}`) }),
    `column ${columnId}`
  ).toHaveCount(1);
}

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
