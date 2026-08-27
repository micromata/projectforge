import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { userFormat } from "./fixtures/format";

/**
 * The calendar settings' "show timesheets" control, against the live backend.
 *
 * The rule (TimesheetDao.showTimesheetsOfOtherUsers): only HR and finance users may pick *another*
 * user's timesheets, so they get the user autocomplete. Everyone else may only toggle their own, so
 * they get a plain checkbox and no way to reach a foreign user. The gate is enforced server side too
 * (the change endpoint and the event query both clamp to self), but what a normal user is *offered*
 * is what this spec pins.
 *
 * A live backend, and the first navigation to a route additionally waits for the dev server to
 * compile it — hence the raised timeout.
 */
test.describe.configure({ timeout: 120_000 });

/** The trigger of the calendar settings dialog and the timesheets control inside it. */
async function openSettings(page: import("@playwright/test").Page) {
  const format = await userFormat(page);
  await goto(page, "/calendar");
  await expect(page.locator(".pf-calendar")).toBeVisible();
  await page
    .getByRole("button", { name: format.t("calendar.view.settings.tooltip") })
    .click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  return { dialog, timesheetsLabel: format.t("calendar.option.timesheets") };
}

test.describe("calendar timesheet-user setting", () => {
  test.describe("without HR or finance rights", () => {
    test.skip(
      !hasRole("normalo-user"),
      "no normalo-user in this instance's testAccounts.txt"
    );

    test("offers only a show-timesheets checkbox, not a user picker", async ({
      page,
    }) => {
      // Not `loggedInPage`: that fixture logs in as the account with every right, which is exactly the
      // account that must *not* be looked at here.
      await login(page, "/next/", "normalo-user");
      const { dialog, timesheetsLabel } = await openSettings(page);

      // The plain checkbox is there ...
      await expect(dialog.locator("#calendar-show-timesheets")).toBeVisible();
      // ... and the user autocomplete (a combobox named by the same label) is not.
      await expect(
        dialog.getByRole("combobox", { name: timesheetsLabel })
      ).toHaveCount(0);
    });
  });

  test.describe("with finance rights", () => {
    test.skip(
      !hasRole("finance-user"),
      "no finance-user in this instance's testAccounts.txt"
    );

    test("offers a user picker for other users' timesheets", async ({
      page,
    }) => {
      await login(page, "/next/", "finance-user");
      const { dialog, timesheetsLabel } = await openSettings(page);

      // The user autocomplete is offered ...
      await expect(
        dialog.getByRole("combobox", { name: timesheetsLabel })
      ).toBeVisible();
      // ... in place of the plain checkbox.
      await expect(dialog.locator("#calendar-show-timesheets")).toHaveCount(0);
    });
  });
});
