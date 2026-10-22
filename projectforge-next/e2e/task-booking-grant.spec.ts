import type { APIRequestContext } from "@playwright/test";
import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { userFormat } from "./fixtures/format";
import {
  MARKER,
  createTask,
  markAsDeleted,
  writeHeaders,
  type SeededTask,
} from "./fixtures/seed";
import { loggedInRequest } from "./fixtures/session";

/**
 * Ad hoc booking grants against the live backend: who may edit the time sheets of others on a task (here the
 * full access account) grants a user without any group (`normalo-user`) the booking on the run's own subtask;
 * the user books on it, sees it and its parent in the structure tree, and books no more once it is revoked.
 *
 * Writes only the run's own task, grant and time sheet (marked as deleted at the end). The sheet is dated
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

/** Books the helper's sheet on the task, yesterday 02:00 - 02:15 UTC; answers the response. */
async function book(
  request: APIRequestContext,
  taskId: number,
  userId: number,
  description: string
) {
  const day = isoDate(-1);
  return request.put("/rs/timesheet/saveorupdate", {
    headers: await writeHeaders(request),
    data: {
      data: {
        task: { id: taskId },
        user: { id: userId },
        startTime: `${day}T02:00:00.000Z`,
        stopTime: `${day}T02:15:00.000Z`,
        description,
      },
    },
  });
}

test.describe("task booking grants", { tag: "@lane-task" }, () => {
  test.skip(!hasRole("normalo-user"), "No normalo-user on this instance.");

  test("a grant lets a user without groups book on the granted task only", async ({
    page,
    baseURL,
  }) => {
    await login(page);
    const task: SeededTask = await createTask(page.request);
    const helper = await loggedInRequest(baseURL ?? "", "normalo-user");
    const helperId = await userIdOf(helper);
    const description = `${MARKER} booking grant ${task.suffix}`;
    let sheetId: number | undefined;
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
          taskId: task.child.id,
          userId: helperId,
          validFrom: isoDate(-2),
          validUntil: isoDate(2),
          asInvitation: false,
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

      // The granting user sees the grant in the task form's own section.
      const format = await userFormat(page);
      await goto(page, `/task/${task.child.id}`);
      await page
        .getByRole("tab", { name: format.t("task.bookingGrants._") })
        .click();
      await expect(
        page.getByRole("cell", { name: grant.userName ?? "", exact: true })
      ).toBeVisible();

      // The helper books on the granted task, but not on its parent.
      const booked = await book(helper, task.child.id, helperId, description);
      expect(booked.ok(), await booked.text()).toBe(true);
      expect(
        (await book(helper, task.id, helperId, `${description} parent`)).ok()
      ).toBe(false);
      const list = await helper.post("/rs/timesheet/listPage", {
        headers: await writeHeaders(helper),
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
      sheetId = resultSet.find((row) => row.description === description)?.id;
      expect(sheetId).toBeDefined();

      // In the tree the helper sees the granted task and its parent, titles only.
      const helperPage = await page.context().browser()!.newPage();
      await login(helperPage, "normalo-user", "/taskTree");
      await expect(helperPage.getByText(task.child.title)).toBeVisible();
      await expect(
        helperPage.getByText(task.title, { exact: true })
      ).toBeVisible();
      await helperPage.close();

      // Revoked: no more bookings, the own sheet stays readable.
      const revoked = await page.request.post(
        `/rs/taskBookingGrant/revoke?id=${grant.id}`,
        { headers: await writeHeaders(page.request) }
      );
      expect(((await revoked.json()) as { success: boolean }).success).toBe(
        true
      );
      openGrantId = undefined;
      expect(
        (
          await book(helper, task.child.id, helperId, `${description} late`)
        ).ok()
      ).toBe(false);
      expect(
        (
          await helper.get(`/rs/timesheet/${sheetId}`, {
            headers: { "X-PF-Frontend": "next" },
          })
        ).ok()
      ).toBe(true);
    } finally {
      if (openGrantId != null) {
        await page.request.post(
          `/rs/taskBookingGrant/revoke?id=${openGrantId}`,
          {
            headers: await writeHeaders(page.request),
          }
        );
      }
      if (sheetId != null)
        await markAsDeleted(page.request, "timesheet", sheetId);
      await helper.dispose();
    }
  });
});
