import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import type { Page } from "@playwright/test";

/**
 * The shared date input (components/shared/date-input.tsx) on the books list, whose "Datum" filter
 * (BookDO.lendOutDate, filterType DATE) is a range of two of them.
 *
 * Nothing is saved: the pill's popover is opened, typed into and abandoned, so the account's stored
 * filter stays as it is. Every expectation about the layout of a date or about a label is derived
 * from the logged-in user (see fixtures/format.ts) — spelling out "dd.MM.yyyy" here would pass only
 * for a German account and hide exactly the bug this component was written for.
 */
test.describe("date input", () => {
  test("takes a date in the user's layout and offers its mask", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const field = await openDateFilter(page, format);

    await expect(field).toHaveAttribute(
      "placeholder",
      format.context.datePattern!
    );

    // Typed the way the user reads it, so the input has to accept its own layout.
    const typed = format.date("2024-03-07");
    await field.fill(typed);
    await field.blur();
    await expect(field).toHaveValue(typed);
  });

  test("steps a day with the arrow keys", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    const field = await openDateFilter(page, format);

    await field.fill(format.date("2024-03-07"));
    await field.press("ArrowUp");
    await expect(field).toHaveValue(format.date("2024-03-08"));
    await field.press("ArrowDown");
    await field.press("ArrowDown");
    await expect(field).toHaveValue(format.date("2024-03-06"));
  });

  test("puts back the last date when the text is not one", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const field = await openDateFilter(page, format);
    const valid = format.date("2024-03-07");

    await field.fill(valid);
    await field.blur();
    await field.fill("nonsense");
    await field.blur();
    await expect(field).toHaveValue(valid);
  });

  test("clears the date without emptying the text by hand", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const field = await openDateFilter(page, format);

    await field.fill(format.date("2024-03-07"));
    await field.blur();

    // The button inside the field, named after the field it belongs to — the name has to carry that,
    // otherwise the list's own "reset filter" would answer to it too.
    await page
      .getByRole("button", {
        name: `${format.t("reset")}: ${fromFieldName(format)}`,
      })
      .click();
    await expect(field).toHaveValue("");

    // …and so does the calendar's, which is the way out when the field is not focused.
    await field.fill(format.date("2024-03-07"));
    await field.blur();
    const calendar = await openCalendar(page, format);
    await calendar
      .getByRole("button", { name: format.t("reset"), exact: true })
      .click();
    await expect(field).toHaveValue("");
  });

  test("starts the week on the day of the user's settings", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openDateFilter(page, format);
    const calendar = await openCalendar(page, format);

    // The first cell of the grid, whose accessible name react-day-picker builds from the locale and
    // opens with the weekday. Which weekday that has to be comes from the account
    // (userData.firstDayOfWeekSunday0), so the expectation is derived from it rather than named.
    await expect(calendar.getByRole("gridcell").first()).toHaveAccessibleName(
      new RegExp(`^${firstWeekday(format)}`)
    );
  });

  test("sets today from the calendar", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    const field = await openDateFilter(page, format);
    const calendar = await openCalendar(page, format);

    await calendar
      .getByRole("button", { name: format.t("calendar.today"), exact: true })
      .click();

    await expect(field).toHaveValue(format.date(new Date()));
  });
});

/**
 * Opens the books list's DATE filter and returns its "from" input.
 *
 * `lendOutDate` is no default filter, so the pill has to be added from the "+" chip first — which
 * also means nothing is stored: an unsaved pill disappears when its popover closes. Field and input
 * are addressed through the label the backend gives them (BookDO.lendOutDate is labelled "date"),
 * never through a text written out here (see RangeField).
 */
async function openDateFilter(page: Page, format: UserFormat) {
  await goto(page, "/books");
  const label = format.t("date._");

  await page.getByRole("button", { name: format.t("filter.addField") }).click();
  await page.getByRole("option", { name: label, exact: true }).click();

  return page.getByRole("textbox", { name: fromFieldName(format) });
}

/** Accessible name of the range's "from" input, as RangeField composes it. */
function fromFieldName(format: UserFormat): string {
  return `${format.t("date._")}: ${format.t("filter.value")}`;
}

/**
 * Opens the calendar of that first input and returns its popover — the last of them, since the pill
 * whose popover holds the input is one too.
 */
async function openCalendar(page: Page, format: UserFormat) {
  const popover = page.getByRole("dialog").last();
  await page
    .getByRole("button", { name: format.t("calendar.chooseDate") })
    .first()
    .click();
  await expect(popover.getByRole("grid")).toBeVisible();
  return popover;
}

/** The name of the weekday the user's week starts on, in their language. */
function firstWeekday({ context }: UserFormat): string {
  // Any week works; 2024-03-03 is a Sunday, so adding the setting's index lands on that weekday.
  const day = new Date(2024, 2, 3 + (context.weekStartsOn ?? 0));
  return new Intl.DateTimeFormat(context.locale, { weekday: "long" }).format(
    day
  );
}
