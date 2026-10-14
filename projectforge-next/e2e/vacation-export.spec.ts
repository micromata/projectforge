import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The team vacation calendar against the live backend (see VacationExportRest).
 *
 * Nothing is written but the choice of employees and groups, which the backend stores per user: the one stored
 * before is put back at the end. In the vacation lane, as it reads the vacation entries.
 */
test.describe("team vacation calendar", { tag: "@lane-vacation" }, () => {
  test("shows the vacations per quarter, pages and exports them as Excel", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const headers = { "X-PF-Frontend": "next" };
    const initial = (await (
      await page.request.get("/rs/vacationExport/initial", { headers })
    ).json()) as {
      employees: { id: number }[];
      groups: { id: number }[];
      periodKind?: string;
    };
    const store = (
      employeeIds: number[],
      groupIds: number[],
      periodKind?: string
    ) =>
      page.request.post("/rs/vacationExport/view", {
        headers: { ...headers, "Content-Type": "application/json" },
        data: {
          employeeIds,
          groupIds,
          periodKind,
          from: "2030-01-01",
          until: "2030-01-01",
        },
      });
    try {
      const employees = (await (
        await page.request.get("/rs/vacationExport/employees?search=", {
          headers,
        })
      ).json()) as { id: number; displayName: string }[];
      test.skip(employees.length === 0, "no employee to pick");
      const employee = employees[0];
      expect((await store([employee.id], [], "quarter")).ok()).toBe(true);

      await goto(page, "/vacationExport?period=quarter&from=2030-04-01");
      await expect(
        page.getByRole("heading", { name: t("vacation.export.title") })
      ).toBeVisible();
      // The stored choice is shown again, and the chart has a row for the employee.
      const chart = page.getByRole("img", { name: t("vacation.export.title") });
      await expect(chart).toBeVisible();
      await expect(chart.getByText(employee.displayName).first()).toBeVisible();
      await expect(chart.getByText(/April 2030/)).toBeVisible();

      await page
        .getByRole("button", {
          name: t("calendar.quickselect.tooltip.selectNextQuarter"),
        })
        .click();
      await expect(page).toHaveURL(/from=2030-07-01/);
      await expect(chart.getByText(/July 2030|Juli 2030/)).toBeVisible();

      const download = page.waitForEvent("download");
      await page.getByRole("button", { name: t("exportAsXls") }).click();
      expect((await download).suggestedFilename()).toMatch(/\.xlsx$/);
    } finally {
      await store(
        initial.employees.map((e) => e.id),
        initial.groups.map((g) => g.id),
        initial.periodKind
      );
    }
  });
});
