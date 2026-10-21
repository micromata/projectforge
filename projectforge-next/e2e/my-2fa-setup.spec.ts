import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import type { My2FASetupInit } from "../components/features/my-2fa-setup/types";

async function fetchInit(page: Page): Promise<My2FASetupInit> {
  const response = await page.request.get("/rs/2FASetup", {
    headers: { "X-PF-Frontend": "next" },
  });
  expect(response.ok()).toBe(true);
  return (await response.json()) as My2FASetupInit;
}

/**
 * The 2FA setup (/next/2FASetup, My2FASetupRest) against the live backend. Read-only: every change needs a
 * recent second factor, which a test can't provide, so nothing is enabled, saved, registered or deleted (the
 * confirmations are cancelled).
 */
test.describe("2FA setup", { tag: "@parallel" }, () => {
  test("shows the sections of the user's factors", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const init = await fetchInit(page);

    await goto(page, "/2FASetup");
    await expect(
      page.getByRole("heading", { name: t("user.My2FA.setup.title") })
    ).toBeVisible();
    await expect(
      page.getByText(t("user.My2FA.setup.authenticator.title"), {
        exact: true,
      })
    ).toBeVisible();
    await expect(
      page.getByText(t("webauthn.title"), { exact: true })
    ).toBeVisible();
    await expect(
      page.getByText(t("user.My2FA.setup.sms.info.title"), { exact: true })
    ).toBeVisible({ visible: init.smsConfigured });
    await expect(
      page.getByRole("button", { name: t("user.My2FA.setup.check2FA") })
    ).toBeVisible();
    for (const entry of init.webAuthnEntries) {
      await expect(
        page.getByRole("button", {
          name: entry.displayName || t("untitled"),
          exact: true,
        })
      ).toBeVisible();
    }
  });

  test("asks before changing the authenticator app", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const init = await fetchInit(page);
    const label = init.authenticatorConfigured
      ? t("user.My2FA.setup.disableAuthenticatorApp._")
      : t("user.My2FA.setup.enableAuthenticatorApp._");

    await goto(page, "/2FASetup");
    await page.getByRole("button", { name: label, exact: true }).click();
    const dialog = page.getByRole("alertdialog");
    await expect(dialog).toBeVisible();
    await dialog.getByRole("button", { name: t("cancel") }).click();
    await expect(dialog).toBeHidden();
  });

  test("is linked from my account", async ({ loggedInPage: page }) => {
    const { t } = await userFormat(page);
    await goto(page, "/myAccount");
    await page
      .getByRole("link", { name: t("menu.2FASetup"), exact: true })
      .first()
      .click();
    await expect(page).toHaveURL(/\/next\/2FASetup$/);
  });
});
