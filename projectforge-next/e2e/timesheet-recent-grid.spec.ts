import type { Page } from "@playwright/test";
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
 * The recently used entries of the time sheet form are the shared grid (RecentTimesheetsPopover): filtered
 * and sorted per column, its columns hideable, the layout stored in the user's prefs under a slot of its own.
 *
 * Two sheets on a task of the run's own, far in the future, become the user's newest recent entries; the
 * search narrows the table to them, so other entries of the account don't matter. Not `@parallel`: the
 * recent list and its layout are per user, and the hidden column is stored for the account.
 */
test.describe.configure({ timeout: 120_000 });

test.describe("timesheet recent entries grid", () => {
  let task: SeededTask;
  const timesheetIds: number[] = [];

  async function book(page: Page, userId: number, hour: number, label: string) {
    const res = await page.request.put(`/rs/${TIMESHEET_ENTITY}/saveorupdate`, {
      headers: await writeHeaders(page.request),
      data: {
        data: {
          task: { id: task.child.id },
          user: { id: userId },
          startTime: `2099-06-01T${String(hour).padStart(2, "0")}:00:00.000Z`,
          stopTime: `2099-06-01T${String(hour + 1).padStart(2, "0")}:00:00.000Z`,
          location: `${MARKER} ${label} ${task.suffix}`,
          description: `${MARKER} recent ${label} ${task.suffix}`,
        },
      },
    });
    expect(res.ok(), await res.text()).toBe(true);
  }

  test.beforeEach(async ({ loggedInPage: page }) => {
    task = await createTask(page.request);
    const status = await page.request.get("/rs/userStatus", {
      headers: { "X-PF-Frontend": "next" },
    });
    const { userData } = (await status.json()) as {
      userData: { userId: number };
    };
    await book(page, userData.userId, 8, "alpha");
    await book(page, userData.userId, 10, "beta");
    // The save answers with the calendar redirect, not the new id: read back for the cleanup.
    const list = await page.request.post(`/rs/${TIMESHEET_ENTITY}/listPage`, {
      headers: await writeHeaders(page.request),
      data: {
        filter: { entries: [{ field: "task", value: { id: task.child.id } }] },
        offset: 0,
        limit: 10,
        doNotStore: true,
      },
    });
    const { resultSet } = (await list.json()) as {
      resultSet: { id: number }[];
    };
    timesheetIds.push(...resultSet.map((row) => row.id));
    expect(timesheetIds).toHaveLength(2);
  });

  test.afterEach(async ({ loggedInPage: page }) => {
    for (const id of timesheetIds.splice(0)) {
      await markAsDeleted(page.request, TIMESHEET_ENTITY, id);
    }
    // Back to the default layout, so the account's next run (and the user) starts from the columns as declared.
    await page.request.post(
      `/rs/${TIMESHEET_ENTITY}/recentList/setColumnStates`,
      {
        headers: await writeHeaders(page.request),
        data: { columnVisibility: {}, sorting: [], columnOrder: [] },
      }
    );
  });

  test("filters, sorts and hides columns, and keeps the layout", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const alpha = `${MARKER} recent alpha ${task.suffix}`;
    const beta = `${MARKER} recent beta ${task.suffix}`;

    const openRecent = async () => {
      await page
        .getByRole("button", { name: t("timesheet.recent"), exact: true })
        .click();
      const box = page.getByRole("dialog").filter({
        has: page.getByRole("textbox", { name: t("search._"), exact: true }),
      });
      await box
        .getByRole("textbox", { name: t("search._"), exact: true })
        .fill(task.suffix);
      return box;
    };
    const descriptions = (box: ReturnType<Page["getByRole"]>) =>
      box
        .getByRole("cell")
        .filter({ hasText: new RegExp(`recent (alpha|beta) ${task.suffix}`) });

    await goto(page, "/timesheet/new");
    let box = await openRecent();
    // Newest first, as the backend lists them.
    await expect(descriptions(box)).toHaveText([beta, alpha], {
      timeout: 30_000,
    });

    // Sorted by a click on the description's header, ascending.
    const descriptionHeader = box
      .getByRole("columnheader")
      .filter({ hasText: t("description") });
    await descriptionHeader.click();
    await expect(descriptions(box)).toHaveText([alpha, beta]);

    // A selection filter on the location keeps the one entry.
    await box
      .getByRole("columnheader")
      .filter({ hasText: t("timesheet.location") })
      .getByRole("button", { name: t("filter.title"), exact: true })
      .click();
    const filter = page.getByRole("dialog").filter({
      has: page.getByRole("button", { name: t("filter.selectNone") }),
    });
    await filter.getByRole("button", { name: t("filter.selectNone") }).click();
    await filter
      .locator("label")
      .filter({ hasText: `${MARKER} beta ${task.suffix}` })
      .getByRole("checkbox")
      .click();
    await expect(descriptions(box)).toHaveText([beta]);
    await page.keyboard.press("Escape");

    // The tag column hidden in the column panel.
    await box
      .getByRole("button", { name: t("columns._"), exact: true })
      .click();
    const tagHeader = box
      .getByRole("columnheader")
      .filter({ hasText: t("timesheet.tag") });
    await expect(tagHeader).toHaveCount(1);
    await page
      .getByRole("checkbox", { name: t("timesheet.tag"), exact: true })
      .click();
    await expect(tagHeader).toHaveCount(0);

    // Picking a row still applies it and closes the box.
    await page.keyboard.press("Escape");
    await descriptions(box).first().click();
    await expect(box).toHaveCount(0);

    // After a reload: the layout (hidden tag, sort) is restored, the column filter is not.
    await page.waitForTimeout(1_500); // the layout is posted debounced
    await page.reload();
    box = await openRecent();
    await expect(descriptions(box)).toHaveText([alpha, beta], {
      timeout: 30_000,
    });
    await expect(
      box.getByRole("columnheader").filter({ hasText: t("timesheet.tag") })
    ).toHaveCount(0);
  });
});
