import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import type { MyAccountInit } from "../components/features/my-account/types";

async function fetchInit(page: Page): Promise<MyAccountInit> {
  const response = await page.request.get("/rs/myAccount", {
    headers: { "X-PF-Frontend": "next" },
  });
  expect(response.ok()).toBe(true);
  return (await response.json()) as MyAccountInit;
}

async function writeHeaders(page: Page): Promise<Record<string, string>> {
  const status = await page.request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  return {
    "X-PF-Frontend": "next",
    "X-PF-CSRF-Token": csrfToken,
    "Content-Type": "application/json",
  };
}

/** The row of an access token: its value (a group named by the label) and the buttons beside it. */
function tokenRow(page: Page, label: string) {
  return page.getByRole("group", { name: label }).locator("xpath=..");
}

function davLabel(format: UserFormat): string {
  return format.t("user.authenticationToken.dav_token._");
}

/**
 * The my-account page (/next/myAccount, MyAccountRest) against the live backend. Read-only: nothing is
 * saved, renewed or logged out (the confirmations are cancelled).
 */
test.describe("my account", { tag: "@parallel" }, () => {
  test("shows the account, hides the tokens until revealed", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    const init = await fetchInit(page);

    await goto(page, "/myAccount");
    await expect(
      page.getByRole("heading", { name: t("user.myAccount.title.edit") })
    ).toBeVisible();
    await expect(page.getByText(init.data.username!).first()).toBeVisible();

    const davToken = init.data.tokens?.find((it) => it.type === "DAV_TOKEN");
    expect(davToken?.token).toBeTruthy();
    const label = davLabel(format);
    const value = page.getByRole("group", { name: label });
    await expect(value).not.toContainText(davToken!.token!);
    await value
      .getByRole("button", { name: `${t("secret.show")}: ${label}` })
      .click();
    await expect(value).toContainText(davToken!.token!);

    await expect(
      page.getByText(t("login.stayLoggedIn.devices.tooltip"))
    ).toBeVisible();
  });

  test("asks before renewing a token and shows its usage", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    const before = await fetchInit(page);

    await goto(page, "/myAccount");
    const row = tokenRow(page, davLabel(format));
    await row
      .getByRole("button", { name: t("user.authenticationToken.renew._") })
      .click();
    const confirm = page.getByRole("alertdialog");
    await expect(confirm).toContainText(
      t("user.authenticationToken.renew.securityQuestion")
    );
    await confirm.getByRole("button", { name: t("cancel") }).click();
    await expect(confirm).toHaveCount(0);

    await row
      .getByRole("button", {
        name: t("user.authenticationToken.button.showUsage._"),
      })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(
      dialog.getByRole("heading", { name: davLabel(format) })
    ).toBeVisible();
    await page.keyboard.press("Escape");

    // Cancelled: the token is still the old one.
    const after = await fetchInit(page);
    expect(after.data.tokens).toEqual(before.data.tokens);
  });

  test("refuses a token type that isn't an access token", async ({
    loggedInPage: page,
  }) => {
    const response = await page.request.get(
      "/rs/myAccount/tokenInfo?token=STAY_LOGGED_IN_KEY",
      { headers: { "X-PF-Frontend": "next" } }
    );
    expect(response.status()).toBe(400);
  });
});

/**
 * Writes on the account of the test user (a setting, a token), so not in parallel with the specs reading
 * them. The setting is restored afterwards.
 */
test.describe("my account (writing)", () => {
  test("saves a setting and stays on the page", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const init = await fetchInit(page);
    const original = init.data.excelDateFormat ?? null;
    const target = init.options.excelDateFormats.find(
      (option) => option.value !== original
    );
    expect(target).toBeTruthy();

    try {
      await goto(page, "/myAccount");
      await page.getByRole("combobox", { name: t("dateFormat.xls") }).click();
      await page.getByRole("option", { name: target!.label }).click();
      await page.getByRole("button", { name: t("save") }).click();
      await expect(page.getByText(t("user.myAccount.saved"))).toBeVisible();
      await expect(page).toHaveURL(/\/myAccount/);
      expect((await fetchInit(page)).data.excelDateFormat).toBe(target!.value);
    } finally {
      const current = (await fetchInit(page)).data;
      await page.request.post("/rs/myAccount/save", {
        headers: await writeHeaders(page),
        data: { data: { ...current, excelDateFormat: original } },
      });
    }
    expect((await fetchInit(page)).data.excelDateFormat ?? null).toBe(original);
  });

  test("renews the DAV token", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    const { t } = format;
    const before = (await fetchInit(page)).data.tokens?.find(
      (it) => it.type === "DAV_TOKEN"
    );

    await goto(page, "/myAccount");
    await tokenRow(page, davLabel(format))
      .getByRole("button", { name: t("user.authenticationToken.renew._") })
      .click();
    await page
      .getByRole("alertdialog")
      .getByRole("button", { name: t("user.authenticationToken.renew._") })
      .click();
    await expect(
      page.getByText(t("user.authenticationToken.renew.successful"))
    ).toBeVisible();

    const after = (await fetchInit(page)).data.tokens?.find(
      (it) => it.type === "DAV_TOKEN"
    );
    expect(after?.token).toBeTruthy();
    expect(after?.token).not.toBe(before?.token);
  });
});
