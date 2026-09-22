import { test, expect, goto } from "./fixtures/auth";
import type { Page } from "@playwright/test";

/**
 * The system alert message (a maintenance announcement) reaching every page of this app.
 *
 * This test *writes* global state: the message is shown to every logged-in user of the instance, and
 * there is no REST endpoint for it — it can only be set on the Wicket admin page, which is what the
 * test drives (same session cookie, cookies ignore the port). Hence the `finally`: a failure in the
 * middle must not leave the announcement standing for everyone.
 *
 * The text is the admin's own and is never translated, so spelling it out here is correct — unlike a
 * label, which would have to come from the user's locale (see fixtures/format).
 */
const MESSAGE =
  "Achtung: ProjectForge ist um 13:00 Uhr für ca. 5 Minuten\naufgrund von Wartungsarbeiten nicht erreichbar!";

test("the system alert message is shown on every page until it is cleared", async ({
  loggedInPage: page,
}) => {
  const banner = page.getByTestId("system-alert-message");
  await goto(page, "/");
  await expect(banner).toHaveCount(0);

  try {
    await setAlertMessage(page, MESSAGE);

    // A page change picks it up: the userStatus query is stale after a minute and refetches on mount
    // (see useAuth) — a full reload is not needed.
    await goto(page, "/");
    await expect(banner).toBeVisible();
    // The admin writes into a textarea, so the line break is part of the message.
    await expect(banner).toContainText("für ca. 5 Minuten");
    await expect(banner).toContainText("nicht erreichbar!");

    // And on a list page, which builds its own chrome on top of the same shell.
    await goto(page, "/book");
    await expect(banner).toBeVisible();
  } finally {
    await clearAlertMessage(page);
  }

  await goto(page, "/");
  await expect(banner).toHaveCount(0);
});

const ADMIN_PAGE = "http://localhost:8080/wa/admin";

async function setAlertMessage(page: Page, message: string): Promise<void> {
  await page.goto(ADMIN_PAGE);
  // The first textarea of the page is the alert message's (AdminForm.init).
  await page.locator("textarea").first().fill(message);
  await page
    .getByRole("button", { name: /^set system alert message$/i })
    .click();
  // Wicket shows the message on its own pages right away — so the backend has it.
  await expect(page.locator("div.global-alert-message")).toBeVisible();
}

async function clearAlertMessage(page: Page): Promise<void> {
  await page.goto(ADMIN_PAGE);
  const clear = page.getByRole("button", { name: /^clear alert message$/i });
  // The button exists only while a message is set, and the test may have failed before setting one.
  if (await clear.count()) {
    await clear.click();
    await expect(page.locator("div.global-alert-message")).toHaveCount(0);
  }
}
