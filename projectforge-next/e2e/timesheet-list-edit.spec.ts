import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import {
  createTask,
  markAsDeleted,
  MARKER,
  writeHeaders,
  type SeededTask,
} from "./fixtures/seed";
import { TIMESHEET_ENTITY } from "../components/features/timesheet/timesheet.page";

/**
 * The time sheet list opens a sheet with `?returnTo=/timesheet`, and cancelling the form comes back to
 * the list — not to the calendar, the form's default caller (see EditDef.returnTargets). Wicket's
 * TimesheetEditPage returned to TimesheetListPage the same way.
 *
 * The list is opened by the task jump (`?taskId=`), whose filter is transient: nothing of the stored
 * list state is read or written, hence `@parallel`. The sheet is dated far in the future on a task of
 * the run's own, so it collides with no real booking, and is deleted again afterwards.
 */
test.describe.configure({ timeout: 120_000 });

test.describe("timesheet list → edit", { tag: "@parallel" }, () => {
  let task: SeededTask;
  let timesheetId: number | undefined;
  let description: string;

  test.beforeEach(async ({ loggedInPage: page }) => {
    task = await createTask(page.request);
    const status = await page.request.get("/rs/userStatus", {
      headers: { "X-PF-Frontend": "next" },
    });
    const { userData } = (await status.json()) as {
      userData: { userId: number };
    };
    description = `${MARKER} timesheet ${task.suffix}`;
    // The save answers with the calendar redirect, not the new id (TimesheetEntityRest.onAfterEdit), so
    // the fixture's `insert` can't be used: the id is read back from the list of the run's own task.
    const res = await page.request.put(`/rs/${TIMESHEET_ENTITY}/saveorupdate`, {
      headers: await writeHeaders(page.request),
      data: {
        data: {
          task: { id: task.child.id },
          user: { id: userData.userId },
          startTime: "2099-06-01T08:00:00.000Z",
          stopTime: "2099-06-01T09:00:00.000Z",
          description,
        },
      },
    });
    expect(res.ok(), await res.text()).toBe(true);
    const list = await page.request.post(`/rs/${TIMESHEET_ENTITY}/listPage`, {
      headers: await writeHeaders(page.request),
      data: {
        filter: {
          entries: [{ field: "task", value: { id: task.child.id } }],
        },
        offset: 0,
        limit: 10,
        doNotStore: true,
      },
    });
    const { resultSet } = (await list.json()) as {
      resultSet: { id: number; description?: string }[];
    };
    timesheetId = resultSet.find((row) => row.description === description)?.id;
    expect(timesheetId).toBeDefined();
  });

  test.afterEach(async ({ loggedInPage: page }) => {
    if (timesheetId != null) {
      await markAsDeleted(page.request, TIMESHEET_ENTITY, timesheetId);
      timesheetId = undefined;
    }
  });

  test("opens a sheet from the list and cancel comes back to the list", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/timesheet?taskId=${task.child.id}`);
    const cell = page.getByRole("cell", { name: description, exact: true });
    await expect(cell).toBeVisible({ timeout: 30_000 });
    await cell.click();

    await expect(page).toHaveURL(
      new RegExp(`/timesheet/${timesheetId}\\?returnTo=%2Ftimesheet$`),
      { timeout: 20_000 }
    );
    await page.getByRole("button", { name: t("cancel"), exact: true }).click();
    await expect(page).toHaveURL(/\/timesheet(\?|$)/, { timeout: 20_000 });
  });
});
