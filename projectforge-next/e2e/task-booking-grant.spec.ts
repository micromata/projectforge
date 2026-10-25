import type { APIRequestContext } from "@playwright/test";
import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { userFormat } from "./fixtures/format";
import {
  MARKER,
  createTask,
  insert,
  markAsDeleted,
  writeHeaders,
  type SeededTask,
} from "./fixtures/seed";
import { loggedInRequest } from "./fixtures/session";

/**
 * Ad hoc booking grants against the live backend: who may edit the time sheets of others on a task (here the
 * full access account) grants a user without any group (`normalo-user`) the booking on two subtasks of the
 * run's own task at once; the user books on them, sees them and their parent in the structure tree, and books
 * no more once the grant is revoked. The granting user sees the grant as badge in the tree, the helper doesn't.
 *
 * Writes only the run's own tasks, grant and time sheets (marked as deleted at the end). The sheets are dated
 * yesterday at night: a grant allows bookings within its period only, which contains today.
 */
const ISO_DAY = 24 * 60 * 60 * 1000;

function isoDate(offsetDays: number): string {
  return new Date(Date.now() + offsetDays * ISO_DAY).toISOString().slice(0, 10);
}

async function userIdOf(request: APIRequestContext): Promise<number> {
  const status = await request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  return ((await status.json()) as { userData: { userId: number } }).userData
    .userId;
}

/** Books the helper's sheet on the task, yesterday at the given hour for 15 minutes (UTC); answers the response. */
async function book(
  request: APIRequestContext,
  taskId: number,
  userId: number,
  description: string,
  hour = 2
) {
  const day = isoDate(-1);
  const hh = String(hour).padStart(2, "0");
  return request.put("/rs/timesheet/saveorupdate", {
    headers: await writeHeaders(request),
    data: {
      data: {
        task: { id: taskId },
        user: { id: userId },
        startTime: `${day}T${hh}:00:00.000Z`,
        stopTime: `${day}T${hh}:15:00.000Z`,
        description,
      },
    },
  });
}

test.describe("task booking grants", { tag: "@lane-task" }, () => {
  test.skip(!hasRole("normalo-user"), "No normalo-user on this instance.");

  test("a grant lets a user without groups book on the granted tasks only", async ({
    page,
    baseURL,
  }) => {
    await login(page);
    const task: SeededTask = await createTask(page.request);
    const secondTitle = `${MARKER} second subtask ${task.suffix}`;
    const secondId = await insert(page.request, "task", {
      title: secondTitle,
      status: "O",
      parentTask: { id: task.id },
    });
    const helper = await loggedInRequest(baseURL ?? "", "normalo-user");
    const helperId = await userIdOf(helper);
    const description = `${MARKER} booking grant ${task.suffix}`;
    const sheetIds: number[] = [];
    // Revoked at the end, or in `finally` if the test fails before: a grant left over is shown to the helper.
    let openGrantId: number | undefined;
    try {
      // Nothing to book before the grant.
      expect(
        (await book(helper, task.child.id, helperId, description)).ok()
      ).toBe(false);

      const granted = await page.request.post("/rs/taskBookingGrant/grant", {
        headers: await writeHeaders(page.request),
        data: {
          taskId: task.id,
          userIds: [helperId],
          positions: [
            { taskId: task.child.id, kost2Ids: [] },
            { taskId: secondId, kost2Ids: [] },
          ],
          validFrom: isoDate(-2),
          validUntil: isoDate(2),
        },
      });
      const result = (await granted.json()) as {
        success: boolean;
        message?: string;
        info: { grants: { id: number; userName?: string }[] };
      };
      expect(result.success, result.message).toBe(true);
      const grant = result.info.grants[0];
      openGrantId = grant.id;

      // The granting user sees the grant as badge in the tree, and in the task form's own section.
      const format = await userFormat(page);
      await goto(page, `/taskTree?highlightId=${secondId}`);
      await expect(
        page
          .getByRole("row")
          .filter({ hasText: secondTitle })
          .getByLabel(grant.userName ?? "", { exact: false })
      ).toBeVisible();
      await goto(page, `/task/${task.id}`);
      await page
        .getByRole("tab", { name: format.t("task.bookingGrants._") })
        .click();
      await expect(
        page.getByRole("cell", { name: grant.userName ?? "", exact: true })
      ).toBeVisible();

      // The helper books on both granted tasks, but not on their parent.
      const booked = await book(helper, task.child.id, helperId, description);
      expect(booked.ok(), await booked.text()).toBe(true);
      const bookedSecond = await book(
        helper,
        secondId,
        helperId,
        `${description} second`,
        3
      );
      expect(bookedSecond.ok(), await bookedSecond.text()).toBe(true);
      expect(
        (await book(helper, task.id, helperId, `${description} parent`, 4)).ok()
      ).toBe(false);
      const list = await helper.post("/rs/timesheet/listPage", {
        headers: await writeHeaders(helper),
        data: {
          filter: {
            entries: [{ field: "task", value: { id: task.id } }],
          },
          offset: 0,
          limit: 10,
          doNotStore: true,
        },
      });
      const { resultSet } = (await list.json()) as {
        resultSet: { id: number; description?: string }[];
      };
      resultSet
        .filter((row) => row.description?.startsWith(description))
        .forEach((row) => sheetIds.push(row.id));
      expect(sheetIds).toHaveLength(2);

      // In the tree the helper sees the granted tasks and their parent, titles only, and no badge.
      const helperPage = await page.context().browser()!.newPage();
      await login(helperPage, "normalo-user", "/taskTree");
      await expect(helperPage.getByText(task.child.title)).toBeVisible();
      await expect(helperPage.getByText(secondTitle)).toBeVisible();
      await expect(
        helperPage.getByText(task.title, { exact: true })
      ).toBeVisible();
      await expect(
        helperPage
          .getByRole("row")
          .filter({ hasText: secondTitle })
          .getByLabel(grant.userName ?? "", { exact: false })
      ).toHaveCount(0);
      await helperPage.close();

      // Revoked: no more bookings, the own sheet stays readable.
      const revoked = await page.request.post(
        `/rs/taskBookingGrant/revoke?id=${grant.id}&taskId=${task.id}`,
        { headers: await writeHeaders(page.request) }
      );
      expect(((await revoked.json()) as { success: boolean }).success).toBe(
        true
      );
      openGrantId = undefined;
      expect(
        (await book(helper, secondId, helperId, `${description} late`, 5)).ok()
      ).toBe(false);
      expect(
        (
          await helper.get(`/rs/timesheet/${sheetIds[0]}`, {
            headers: { "X-PF-Frontend": "next" },
          })
        ).ok()
      ).toBe(true);
    } finally {
      if (openGrantId != null) {
        await page.request.post(
          `/rs/taskBookingGrant/revoke?id=${openGrantId}&taskId=${task.id}`,
          {
            headers: await writeHeaders(page.request),
          }
        );
      }
      for (const id of sheetIds) {
        await markAsDeleted(page.request, "timesheet", id);
      }
      await helper.dispose();
    }
  });
});
