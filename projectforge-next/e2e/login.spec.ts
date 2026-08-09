import { test as base, expect } from "@playwright/test";
import { login, goto, waitForHydration } from "./fixtures/auth";
import { readCredentials } from "./fixtures/credentials";
import { locales, translate } from "./fixtures/format";

/**
 * The login of `/next/login` — since the UILayout pages were deleted (LoginPageRest,
 * PasswordForgottenPageRest, PasswordResetPageRest, My2FAPublicServicesRest) it is the only login of
 * the whole application, for Wicket and the legacy React app as well. There is no fallback left, so
 * the flow needs this net.
 *
 * `base` instead of the `test` of fixtures/auth: these tests log in themselves (or deliberately
 * fail to), the `loggedInPage` fixture would already have done it.
 */
const test = base;

test.describe("login", () => {
  test("keeps the user on the form and names the reason on a wrong password", async ({
    page,
  }) => {
    const { username } = readCredentials();
    await goto(page, "/login");
    await waitForHydration(page);
    await page.fill("#username", username);
    // Deliberately wrong. The account itself stays usable: LoginProtection only delays the *next*
    // attempt of this user (and the delay starts at a second, see LoginProtectionTest).
    await page.fill("#password", "definitely-not-the-password");
    await page.locator('button[type="submit"]').click();

    // The message is the server's (LoginResultStatus.localizedMessage), so it is looked up in the
    // catalogs rather than spelled out. Which one applies is the browser's Accept-Language
    // (LocaleFilter) - there is no session yet to ask - so any of them counts.
    // By tone, not by role: the message of the day is a `role="alert"` of this form as well, and so
    // is Next's route announcer.
    const alert = page.locator('[role="alert"][data-tone="error"]');
    await expect(alert).toBeVisible();
    expect(loginFailedMessages()).toContain(
      (await alert.textContent())?.trim()
    );
    await expect(page).toHaveURL(/\/login/);
    // No session was created, so the app is still out of reach.
    await goto(page, "/books/");
    await expect(page).toHaveURL(/\/login/);

    // Wait out the time penalty this test just earned for the account: LoginProtection increments
    // it on the *first* failure for a username (numberOfFailedLoginsBeforeIncrementing = 1) by
    // loginTimeOffsetScale = 1s, and it expires a second after the last failure. Without this the
    // next test's correct password is answered with LOGIN_TIME_OFFSET and its login fails.
    await page.waitForTimeout(1_500);
  });

  test("returns to the page the user was sent away from", async ({ page }) => {
    // A deep link of this app, as WicketUserFilter/actions/authentication.js hand it over.
    await login(page, "/next/books/");
    await expect(page).toHaveURL(/\/next\/books/);
  });

  test("drops a returnUrl pointing at another host", async ({ page }) => {
    // An open redirect would be a convincing phishing hop: the victim really did log in here.
    // Rejected on both sides (lib/menu-url.ts and LoginServiceRest.sanitizeRedirectUrl), so the
    // login falls back to its own target instead.
    await login(page, "https://evil.example/phish");
    await expect(page).not.toHaveURL(/evil\.example/);
    await expect(page).not.toHaveURL(/\/login/);
  });

  test("confirms a password reset request without revealing the account", async ({
    page,
  }) => {
    const { username } = readCredentials();
    await goto(page, "/password-forgotten");
    await waitForHydration(page);
    await page.fill("#usernameEmail", username);
    await page.locator('button[type="submit"]').click();

    // Same confirmation for an existing and a non-existing account (PasswordResetService.sendMail
    // sends in a thread for exactly that reason), naming the *entered* value only. Compared against
    // every shipped language: the server picks it from Accept-Language, there is no session to ask.
    // By tone: Next's route announcer is a `role="alert"` too, and an empty one.
    const alert = page.locator('[role="alert"][data-tone="success"]');
    await expect(alert).toBeVisible();
    expect(mailSentMessages(username)).toContain(
      (await alert.textContent())?.trim()
    );
  });
});

/**
 * The wording of a failed login in every language the app ships, because a failed login has no
 * session to derive the user's one from (fixtures/format.ts needs `userStatus`).
 */
function loginFailedMessages(): string[] {
  return locales().map((locale) =>
    translate(locale)("login.error.loginFailed")
  );
}

/**
 * The confirmation of a requested reset mail, in every shipped language. Rendered by next-intl, not
 * by substituting `{arg0}` by hand: the message quotes the entered value as `''{arg0}''` (the
 * bundle is MessageFormat), and only an ICU formatter turns that into the single apostrophes the
 * page shows.
 */
function mailSentMessages(usernameEmail: string): string[] {
  return locales().map((locale) =>
    translate(locale)("password.forgotten.mailSentTo", { arg0: usernameEmail })
  );
}
