import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The team vacation calendar against the live backend (see VacationExportRest).
 *
 * Nothing is written but the choice of employees and groups, which the backend stores per user: the one stored
 * before is put back at the end. In the vacation lane, as it reads the vacation entries.
 *
 * Put back is the choice as `initial` answers it, i.e. without the ids the user may no longer pick (deactivated
 * colleagues, groups left). No endpoint answers the raw stored ids, and none is added for a test: those ids are
 * neither shown nor applied by the page anyway (VacationExportRest filters them on every read), so dropping them
 * changes nothing the user could see. The favorites are not touched.
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
      dayWidth?: string;
    };
    const store = (
      employeeIds: number[],
      groupIds: number[],
      periodKind?: string,
      dayWidth?: string
    ) =>
      page.request.post("/rs/vacationExport/view", {
        headers: { ...headers, "Content-Type": "application/json" },
        data: {
          employeeIds,
          groupIds,
          periodKind,
          dayWidth,
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
      // A group of names and labelled bars (its own text alternative), not one image.
      const chart = page.getByRole("group", {
        name: t("vacation.export.title"),
      });
      await expect(chart).toBeVisible();
      await expect(chart.getByText(employee.displayName).first()).toBeVisible();
      // Every bar is reached by the keyboard and names its period and status.
      const bars = chart.getByRole("img");
      if ((await bars.count()) > 0) {
        await expect(bars.first()).toHaveAttribute("tabindex", "0");
        await expect(bars.first()).toHaveAccessibleName(/–/);
      }
      await expect(chart.getByText(/April 2030/)).toBeVisible();

      await page
        .getByRole("button", {
          name: t("calendar.quickselect.tooltip.selectNextQuarter"),
        })
        .click();
      await expect(page).toHaveURL(/from=2030-07-01/);
      await expect(chart.getByText(/July 2030|Juli 2030/)).toBeVisible();

      // Fitted to the window, the chart no longer scrolls sideways.
      await page
        .getByRole("combobox", { name: t("vacation.export.dayWidth._") })
        .click();
      await page
        .getByRole("option", { name: t("vacation.export.dayWidth.fit") })
        .click();
      await expect
        .poll(() =>
          chart.evaluate((el) => {
            const scroller = el.parentElement as HTMLElement;
            return scroller.scrollWidth <= scroller.clientWidth;
          })
        )
        .toBe(true);

      const download = page.waitForEvent("download");
      await page.getByRole("button", { name: t("exportAsXls") }).click();
      expect((await download).suggestedFilename()).toMatch(/\.xlsx$/);
    } finally {
      await store(
        initial.employees.map((e) => e.id),
        initial.groups.map((g) => g.id),
        initial.periodKind,
        initial.dayWidth
      );
    }
  });
});
