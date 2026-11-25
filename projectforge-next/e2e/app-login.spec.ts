import { test, expect, goto, loginWithOwnSession } from "./fixtures/auth";
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

  test("asks for the second factor itself when a page of the app needs one", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(
      page,
      `/appLogin?challenge=${CHALLENGE}&state=${STATE}&device=Test%20phone&platform=ios&deviceId=1&max2FAAge=60000`
    );
    await expect(
      page.getByText(t("mobileApp.login.secondFactor"))
    ).toBeVisible();
    await expect(page.getByTestId("app-login-question")).toHaveCount(0);
    await expect(
      page.getByRole("button", { name: t("mobileApp.login.connect") })
    ).toHaveCount(0);
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

/**
 * Alone: the logout of "Other user" also clears the cached preferences of the user (LoginService.logout), which
 * every other spec of this user relies on.
 */
test.describe("App login, other user", { tag: "@isolated" }, () => {
  test("names the logged in user and lets another one log in", async ({
    loggedInPage: page,
  }) => {
    // Ends its session: an own one, the shared session would log out every other test.
    await loginWithOwnSession(page);
    const { t } = await userFormat(page);
    await goto(
      page,
      `/appLogin?challenge=${CHALLENGE}&state=${STATE}&device=Test%20phone&platform=ios`
    );
    await expect(page.getByTestId("app-login-user")).toBeVisible();
    await page
      .getByRole("button", { name: t("mobileApp.login.otherUser") })
      .click();
    // The login keeps the challenge of the app, so the user returns to the question after it.
    await expect(page).toHaveURL(/\/login\?returnUrl=.*challenge/);
  });
});
