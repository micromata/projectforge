import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";

/**
 * The subscription links of ProjectForge's own feeds (`CalendarSubscriptionDialog`,
 * `CalendarSubscriptionInfoPageRest.getInfo`) against the live backend: from the calendar's more menu, the
 * holidays feed shows an absolute url carrying the user's token, a QR code of it and the security advice.
 */

test.describe("calendar subscription links", { tag: "@lane-calendar" }, () => {
  test("shows the holidays feed url with its QR code", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/calendar");
    await page.getByRole("button", { name: format.t("more") }).click();
    await page
      .getByRole("menuitem", {
        name: label(format, "plugins.teamcal.subscription"),
      })
      .click();
    const info = page.waitForResponse(
      (r) => r.url().includes("/rs/calendarSubscription/info") && r.ok()
    );
    await page
      .getByRole("menuitem", {
        name: label(format, "plugins.teamcal.export.holidays"),
      })
      .click();
    const { url, headline } = (await (await info).json()) as {
      url: string;
      headline: string;
    };
    expect(url).toMatch(/^https?:\/\/.+\.ics\?/);

    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("group", { name: headline })).toHaveText(url);
    const qr = dialog.getByRole("img", { name: headline });
    await expect(qr).toBeVisible();
    // Loaded, not just placed: a broken image has no natural width.
    await expect
      .poll(() => qr.evaluate((img: HTMLImageElement) => img.naturalWidth))
      .toBeGreaterThan(0);
  });
});
