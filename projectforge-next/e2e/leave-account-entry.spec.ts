import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { LEAVE_ACCOUNT_ENTRY_METADATA } from "../lib/metadata/leave-account-entry.generated";
import { LEAVE_ACCOUNT_ENTRY_PAGE } from "../components/features/leave-account-entry/leave-account-entry.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The leave account entries against the live backend (see LEAVE_ACCOUNT_ENTRY_PAGE and LeaveAccountEntryEntityRest).
 *
 * The database may be a copy of production: nothing is written. The save test intercepts the save request. In the
 * vacation lane, as the vacation account shows these entries.
 */
test.describe("leaveAccountEntry", { tag: "@lane-vacation" }, () => {
  test("shows the declared columns under the labels of LeaveAccountEntryDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/leaveAccountEntry");

    await expect(
      page.getByRole("heading", {
        name: format.t(LEAVE_ACCOUNT_ENTRY_PAGE.titleKey),
      })
    ).toBeVisible();
    for (const column of LEAVE_ACCOUNT_ENTRY_PAGE.columns) {
      if (column.hiddenByDefault) continue;
      const key = columnHeaderKeyOf(column, LEAVE_ACCOUNT_ENTRY_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: new RegExp(`^${escape(label(format, key))}`),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("posts a new entry on save", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    let saveAttempted = false;
    await page.route("**/rs/leaveAccountEntry/{insert,update}*", (route) => {
      saveAttempted = true;
      return route.abort();
    });
    // The form needs an employee: the one the vacation account shows (a slot database's user may have none).
    const account = (await (
      await page.request.get("/rs/vacationAccount", {
        headers: { "X-PF-Frontend": "next" },
      })
    ).json()) as { employee?: { id: number } };
    const employeeId = account.employee?.id;
    test.skip(
      employeeId === undefined,
      "the vacation account shows no employee"
    );

    // As the vacation account opens it: the employee preset by the backend, today as date.
    await goto(page, `/leaveAccountEntry/new?employee=${employeeId}`);
    await page
      .getByRole("textbox", {
        name: label(format, "vacation.leaveAccountEntry.amount"),
      })
      .fill("1.5");
    await page.getByRole("button", { name: format.t("create") }).click();

    await expect
      .poll(() => saveAttempted, { message: "the save reaches the backend" })
      .toBe(true);
  });
});

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
