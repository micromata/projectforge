import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { insert, markAsDeleted } from "./fixtures/seed";
import { formatNumber } from "../lib/format";

/**
 * The HR view ("Personalplanung", components/features/hr-view/) against the live backend: a planned week
 * of the tests' own shows up as a row, its project as a column, and the employee leads to the planning.
 *
 * The planning is of the logged-in account and of a week of its own per run, far in the future, so no
 * real planning is in the period and a following run does not collide with this one (a user has one
 * planning per week). It is marked as deleted afterwards — the view leaves out deleted entries, and a
 * planning re-added in the same week would revive it anyway (`HRPlanningDao.onInsertOrModify`).
 */
test.describe("HR view", { tag: "@parallel" }, () => {
  test("shows the planned days of a week per project and the rest, linked to the planning", async ({
    loggedInPage: page,
    seedRequest,
    seededProject,
  }) => {
    const status = await seedRequest.get("/rs/userStatus", {
      headers: { "X-PF-Frontend": "next" },
    });
    const { userData } = (await status.json()) as {
      userData: { userId: number };
    };
    const week = futureMonday();
    const planningId = await insert(seedRequest, "hrPlanning", {
      week,
      user: { id: userData.userId },
      entries: [
        { projekt: { id: seededProject.id }, mondayHours: 8, tuesdayHours: 4 },
        { status: "OTHER", fridayHours: 8 },
      ],
    });
    try {
      await goto(page, `/hrList?startDay=${week}&stopDay=${plusDays(week, 6)}`);
      const { t, context } = await userFormat(page);
      const days = (value: number) => formatNumber(value, context, 2);

      const table = page.getByRole("table");
      const header = table.getByRole("columnheader");
      await expect(
        header.filter({ hasText: seededProject.name })
      ).toBeVisible();
      const headers = await header.allInnerTexts();
      const column = (label: string) =>
        headers.findIndex((text) => text.includes(label));

      const link = page.locator(`a[href$="/hrPlanning/${planningId}"]`);
      await expect(link).toBeVisible();
      const row = table.getByRole("row").filter({ has: link });
      const cells = row.getByRole("cell");
      await expect(cells.nth(column(t("sum")))).toHaveText(days(2.5));
      await expect(cells.nth(column(t("rest")))).toHaveText(days(1));
      await expect(cells.nth(column(seededProject.name))).toHaveText(days(1.5));

      await link.click();
      await expect(page).toHaveURL(new RegExp(`/hrPlanning/${planningId}$`));
    } finally {
      await markAsDeleted(seedRequest, "hrPlanning", planningId);
    }
  });
});

/**
 * A Monday of the 2090s and beyond, one week per second of a ~38 year cycle: unique per run without a
 * state, and far from every real planning.
 */
function futureMonday(): string {
  const weeks = Math.floor(Date.now() / 1000) % 2000;
  // 2090-01-02 is a Monday.
  return plusDays("2090-01-02", weeks * 7);
}

function plusDays(iso: string, days: number): string {
  const date = new Date(`${iso}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}
