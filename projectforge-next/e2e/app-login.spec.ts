import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/** Example of RFC 7636, appendix B: a challenge as the app creates it. */
const CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
const STATE = "state-1234567890abcdef";

/**
 * The page the ProjectForge app opens in the system browser (/next/appLogin, AppAuthRest). Nothing is
 * connected: issuing the code needs a fresh second factor, which a test can't provide (see
 * my-2fa-setup.spec.ts), so the 2FA dialog is the furthest a test gets. The code flow itself is covered by
 * AppAuthServiceTest.
 */
test.describe("App login", { tag: "@parallel" }, () => {
  test("asks for the device and demands a second factor", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(
      page,
      `/appLogin?challenge=${CHALLENGE}&state=${STATE}&device=Test%20phone&platform=ios`
    );
    // AuthCard's title is no heading element (shadcn CardTitle is a div).
    await expect(
      page.getByText(t("mobileApp.login.title"), { exact: true })
    ).toBeVisible();
    await expect(page.getByTestId("app-login-question")).toHaveText(
      t("mobileApp.login.question", { arg0: "Test phone" })
    );
    await page
      .getByRole("button", { name: t("mobileApp.login.connect") })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(
      dialog.getByText(t("user.My2FACode.title"), { exact: true })
    ).toBeVisible();
  });

  test("reads the packed parameter, the only one an SSO proxy kept", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const login = Buffer.from(
      JSON.stringify({
        challenge: CHALLENGE,
        state: STATE,
        device: "Test phone",
        platform: "ios",
      })
    ).toString("base64url");
    await goto(page, `/appLogin?login=${login}`);
    await expect(page.getByTestId("app-login-question")).toHaveText(
      t("mobileApp.login.question", { arg0: "Test phone" })
    );
  });

  test("reads the packed parameter of the path, an SSO proxy dropped the query", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const login = Buffer.from(
      JSON.stringify({
        challenge: CHALLENGE,
        state: STATE,
        device: "Test phone",
      })
    ).toString("base64url");
    await goto(page, `/appLogin/${login}`);
    await expect(page.getByTestId("app-login-question")).toHaveText(
      t("mobileApp.login.question", { arg0: "Test phone" })
    );
  });

  test("refuses a link without challenge", async ({ loggedInPage: page }) => {
    const { t } = await userFormat(page);
    await goto(page, `/appLogin?state=${STATE}`);
    await expect(page.getByTestId("app-login-status")).toHaveText(
      t("mobileApp.login.invalid")
    );
  });
});
