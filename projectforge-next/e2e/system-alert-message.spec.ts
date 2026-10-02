import { test, expect, goto } from "./fixtures/auth";
import { writeHeaders } from "./fixtures/seed";
import type { Page } from "@playwright/test";

/**
 * The system alert message (a maintenance announcement) reaching every page of this app.
 *
 * This test *writes* global state: the message is shown to every logged-in user of the instance. It is
 * set and cleared through the endpoints of the System page (SystemRest), the subject here being where
 * it shows rather than the page that sets it. Hence the `finally`: a failure in the middle must not
 * leave the announcement standing for everyone.
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

async function setAlertMessage(page: Page, message: string): Promise<void> {
  const res = await page.request.post("/rs/system/setAlertMessage", {
    headers: await writeHeaders(page.request),
    data: { alertMessage: message },
  });
  expect(res.ok(), `set the alert message: HTTP ${res.status()}`).toBe(true);
}

async function clearAlertMessage(page: Page): Promise<void> {
  const res = await page.request.post("/rs/system/clearAlertMessage", {
    headers: await writeHeaders(page.request),
  });
  expect(res.ok(), `clear the alert message: HTTP ${res.status()}`).toBe(true);
}
