import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { TEAM_CAL_METADATA } from "../lib/metadata/team-cal.generated";
import { TEAM_CAL_PAGE } from "../components/features/teamCal/teamCal.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The team calendars against the live backend (see TEAM_CAL_PAGE and TeamCalEntityRest).
 *
 * The database is a copy of production: nothing is written. The form test intercepts the save.
 */
test.describe("teamCal", { tag: "@lane-teamCal" }, () => {
  test("shows the declared columns under the labels of TeamCalDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/teamCal");

    await expect(
      page.getByRole("heading", { name: format.t(TEAM_CAL_PAGE.titleKey) })
    ).toBeVisible();
    for (const column of TEAM_CAL_PAGE.columns) {
      const key = columnHeaderKeyOf(column, TEAM_CAL_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: new RegExp(`^${escape(label(format, key))}`),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("offers ProjectForge's own feeds in the export menu", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/teamCal");

    await page.getByRole("button", { name: t("export") }).click();
    await page
      .getByRole("menuitem", { name: t("plugins.teamcal.export.holidays._") })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog).toBeVisible();
    // The feed url itself; the QR code and the advice are calendar-subscription.spec.ts's.
    await expect(dialog.getByText(/\/export\/ProjectForge/)).toBeVisible();
  });

  test("shows the feed of a stored calendar and switches its reminders", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/teamCal");
    const rows = page.getByRole("row").filter({ has: page.getByRole("cell") });
    // The list shows a calendar on every database: the test data's, or the ones of production.
    await expect(rows.first()).toBeVisible();
    let saveAttempted = false;
    await page.route("**/rs/teamCal/saveorupdate*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    await rows.first().getByRole("cell").first().click();
    await expect(page).toHaveURL(/\/teamCal\/\d+/);
    const reminders = page.getByRole("checkbox", {
      name: t("plugins.teamcal.export.reminder.checkbox._"),
    });
    await expect(reminders).toBeVisible();
    const qr = page.locator("img[src*='text=']");
    const before = await qr.getAttribute("src");
    await reminders.click();
    await expect(qr).not.toHaveAttribute("src", before!);
    expect(saveAttempted).toBe(false);
  });

  test("redirects the old React list", async ({ loggedInPage: page }) => {
    await page.goto("/react/teamCal");
    await expect(page).toHaveURL(/\/next\/teamCal$/);
  });
});

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
