import { test as base, expect, type Page } from "@playwright/test";
import { goto, waitForHydration } from "./fixtures/auth";
import { locales, translate } from "./fixtures/format";

/**
 * The passkey recommendation beside the 2FA code input of the login. The login answer is mocked: whether
 * the user owns a passkey (`methods.webAuthn`) is the only input of the hint, and a real account would have
 * to be switched to and from a second factor.
 */
const test = base;

const KEY = "user.My2FACode.passkey.recommendation";

async function loginRequiring2FA(page: Page, webAuthn: boolean) {
  await page.route("**/rsPublic/nextLogin", (route) => {
    if (route.request().method() !== "POST") return route.fallback();
    return route.fulfill({
      json: {
        status: "TWO_FACTOR_REQUIRED",
        methods: { otp: true, sms: false, mail: false, webAuthn },
      },
    });
  });
  await goto(page, "/login");
  await waitForHydration(page);
  await page.fill("#username", "someone");
  await page.fill("#password", "irrelevant");
  await page.locator('button[type="submit"]').click();
  await expect(page.locator("#otp-code")).toBeVisible();
}

function recommendations(): string[] {
  return locales().map((l) => translate(l)(KEY));
}

test.describe("2FA passkey recommendation", { tag: "@parallel" }, () => {
  test("is shown to a user without a passkey", async ({ page }) => {
    await loginRequiring2FA(page, false);
    const alert = page.locator('[role="alert"][data-tone="warning"]');
    await expect(alert).toBeVisible();
    expect(recommendations()).toContain(
      (await alert.locator("p").first().textContent())?.trim()
    );
  });

  test("is not shown to a user who has a passkey", async ({ page }) => {
    await loginRequiring2FA(page, true);
    await expect(
      page.locator('[role="alert"][data-tone="warning"]')
    ).toHaveCount(0);
  });

  test("is not shown at the login in the app (its setup page is out of reach there)", async ({
    page,
  }) => {
    await page.addInitScript(() => {
      Object.defineProperty(navigator, "userAgent", {
        get: () =>
          "Mozilla/5.0 ProjectForgeApp/1.5.0 (browser2FA) (openInBrowser)",
      });
    });
    await loginRequiring2FA(page, false);
    await expect(
      page.locator('[role="alert"][data-tone="warning"]')
    ).toHaveCount(0);
  });
});
