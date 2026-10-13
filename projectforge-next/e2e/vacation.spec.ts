import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { formatDateInput } from "../lib/date-parse";
import { VACATION_METADATA } from "../lib/metadata/vacation.generated";
import { VACATION_PAGE } from "../components/features/vacation/vacation.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The vacation entries against the live backend (see VACATION_PAGE and VacationEntityRest).
 *
 * The database is a copy of production: nothing is written. The form test intercepts the save and reads
 * what the form computes through `/rs/vacation/stats`, which writes nothing either (not even the conflict
 * cache). In the vacation lane all the same: a visit of the list writes its loaded filter back, which
 * vacation-list-filter.spec.ts pages and checks.
 */
test.describe("vacation", { tag: "@lane-vacation" }, () => {
  test("shows the declared columns under the labels of VacationDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/vacation");

    await expect(
      page.getByRole("heading", { name: format.t(VACATION_PAGE.titleKey) })
    ).toBeVisible();
    // Through `label`: a key with children of its own (`vacation.status`) holds its text under `_`.
    for (const column of VACATION_PAGE.columns) {
      const key = columnHeaderKeyOf(column, VACATION_METADATA);
      await expect(
        // The label at the start: "Vertretung" is part of "Weitere Vertretungen", and the header's name
        // goes on with its buttons (filter, column width).
        page.getByRole("columnheader", {
          name: startsWith(label(format, key)),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("presets the employee, follows the period and refuses to save without manager and replacement", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    const newEntry = async (query = "") =>
      (await (
        await page.request.get(`/rs/vacation/newEntry${query}`, {
          headers: { "X-PF-Frontend": "next" },
        })
      ).json()) as {
        employee?: { id: number; displayName?: string };
        employeeEditable?: boolean;
        manager?: unknown;
        replacement?: unknown;
      };
    // The own employee, or — for HR staff, which the full access user is, on a database where the user has
    // none — any employee, preset the way the vacation account presets it (`?employee=`).
    let formUrl = "/vacation/new";
    let preset = await newEntry();
    if (!preset.employee && preset.employeeEditable) {
      const employees = (await (
        await page.request.get("/rs/employee/autosearch?search=", {
          headers: { "X-PF-Frontend": "next" },
        })
      ).json()) as { id: number }[];
      if (employees.length > 0) {
        formUrl = `/vacation/new?employee=${employees[0].id}`;
        preset = await newEntry(`?employee=${employees[0].id}`);
      }
    }
    test.skip(!preset.employee, "no employee to enter a vacation for");
    let saveAttempted = false;
    await page.route("**/rs/vacation/saveorupdate*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    await goto(page, formUrl);
    await expect(
      page.getByText(preset.employee!.displayName!).first()
    ).toBeVisible();

    // A week of February without holidays, years ahead, so no stored entry of the user is in the way.
    const { monday, friday } = februaryWeek(new Date().getFullYear() + 3);
    const start = page.getByLabel(label(format, "vacation.startdate"), {
      exact: true,
    });
    const end = page.getByLabel(label(format, "vacation.enddate"), {
      exact: true,
    });
    await start.fill(dateInput(format, monday));
    await start.press("Enter");
    // A start without an end makes it a one-day period (useVacationPeriodClamp).
    await expect(end).toHaveValue(dateInput(format, monday));
    await end.fill(dateInput(format, friday));
    await end.press("Enter");

    const workingDays = page.getByLabel(t("vacation.workingdays"), {
      exact: true,
    });
    await expect(workingDays).toHaveText(/^5([.,]0+)?$/, { timeout: 20_000 });
    await page.getByLabel(label(format, "vacation.halfDayEnd")).check();
    await expect(workingDays).toHaveText(/^4[.,]5$/, { timeout: 20_000 });
    // The chart over the week with three days around it, the vacation itself in the first row.
    const chart = page.getByRole("img", {
      name: t("vacation.chart.ariaLabel"),
    });
    await expect(chart).toBeVisible();
    await expect(chart.getByText(preset.employee!.displayName!)).toBeVisible();

    await page.getByRole("button", { name: t("save") }).click();
    // Only what the preset left empty: the user prefs hold the manager and replacement of the last save.
    const missing = [
      preset.manager ? null : "vacation.manager",
      preset.replacement ? null : "vacation.replacement",
    ].filter((key): key is string => key != null);
    for (const key of missing) {
      await expect(
        page.getByText(
          t("validation.error.fieldRequired", { arg0: label(format, key) })
        )
      ).toHaveCount(1);
    }
    if (missing.length > 0) {
      expect(
        saveAttempted,
        "an entry without manager or replacement must not reach the server"
      ).toBe(false);
    }
  });
});

/** Matches a name beginning with the given text as a whole word. */
function startsWith(text: string): RegExp {
  return new RegExp(`^${text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}(\\s|$)`);
}

/** Monday and Friday (ISO) of the first full week of February of the given year. */
function februaryWeek(year: number): { monday: string; friday: string } {
  const date = new Date(Date.UTC(year, 1, 4));
  // getUTCDay: 0 = Sunday … 6 = Saturday; on to the next Monday (or stay).
  date.setUTCDate(date.getUTCDate() + ((8 - date.getUTCDay()) % 7));
  const monday = date.toISOString().slice(0, 10);
  date.setUTCDate(date.getUTCDate() + 4);
  return { monday, friday: date.toISOString().slice(0, 10) };
}

/**
 * A date as it stands in a date box — through the box's own formatter, not `format.date`: the latter
 * renders in the user's time zone, which would move an ISO day across a zone boundary.
 */
function dateInput(format: UserFormat, iso: string): string {
  return formatDateInput(iso, format.context);
}
