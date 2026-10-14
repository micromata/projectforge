import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The vacation account against the live backend (see VacationAccountRest).
 *
 * Nothing is written: the account is read, a vacation of it opened and left with Cancel. Showing another
 * employee stores them as the one HR staff see next (a user pref), so the one shown before is shown again at the
 * end. In the vacation lane, as the form it opens belongs to the vacation entries.
 */
test.describe("vacation account", { tag: "@lane-vacation" }, () => {
  test("shows the leave account and leads into a vacation and back", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const account = async (query = "") =>
      (await (
        await page.request.get(`/rs/vacationAccount${query}`, {
          headers: { "X-PF-Frontend": "next" },
        })
      ).json()) as {
        employee?: { id: number; displayName?: string };
        hrAccess: boolean;
        years: { year: number; vacations: { id: number }[] }[];
      };
    const before = await account();
    // The full access user is HR staff, often without an employee of their own on a slot database: any employee
    // with a vacation then, as HR staff pick them.
    let employeeId = before.employee?.id;
    let shown = before;
    if (before.hrAccess) {
      const employees = (await (
        await page.request.get("/rs/employee/autosearch?search=", {
          headers: { "X-PF-Frontend": "next" },
        })
      ).json()) as { id: number }[];
      for (const employee of employees.slice(0, 20)) {
        const candidate = await account(`?employeeId=${employee.id}`);
        if (candidate.years.some((y) => y.vacations.length > 0)) {
          employeeId = employee.id;
          shown = candidate;
          break;
        }
      }
    }
    try {
      test.skip(!employeeId, "no employee to show the account of");
      await goto(page, `/vacationAccount?employeeId=${employeeId}`);
      await expect(
        page.getByRole("heading", { name: t("vacation.leaveaccount.title") })
      ).toBeVisible();
      // The current year before the previous one; the next only once something is booked in it.
      const year = new Date().getFullYear();
      for (const y of [year, year - 1]) {
        await expect(
          page.getByRole("columnheader", { name: String(y), exact: true })
        ).toBeVisible();
      }

      const vacation = shown.years.flatMap((y) => y.vacations)[0];
      test.skip(!vacation, "the employee has no vacation in these years");
      await page.locator(`a[href*="/vacation/${vacation.id}?"]`).click();
      await expect(page).toHaveURL(
        new RegExp(`/vacation/${vacation.id}\\?returnTo=%2FvacationAccount`)
      );
      await page
        .getByRole("button", { name: t("cancel"), exact: true })
        .click();
      await expect(page).toHaveURL(/\/vacationAccount/);
      await expect(
        page.getByRole("heading", { name: t("vacation.leaveaccount.title") })
      ).toBeVisible();
    } finally {
      if (before.hrAccess && before.employee) {
        await account(`?employeeId=${before.employee.id}`);
      }
    }
  });
});
