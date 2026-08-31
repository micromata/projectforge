import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import {
  bound,
  filterField,
  listRequest,
  openPill,
  reopenPill,
  resetFilter,
} from "./fixtures/filter-pill";
import {
  periodKindOf,
  periodKindsOf,
  type PeriodKind,
} from "../lib/date-period";
import { todayOf } from "../lib/user-zone";
import { kindName, pickKind, picker } from "./fixtures/period-kind";

const YEAR_TO_DATE = periodKindOf("yearToDate") as PeriodKind;

/** What every list offers unless it says otherwise (see [FilterPeriodKindsProvider]'s default). */
const DEFAULT_KINDS = periodKindsOf([
  "month",
  "termThreeMonths",
  "termYear",
  "yearToDate",
]);

/** What the order book declares instead (see `ORDER_PAGE.filterPeriodKinds`). */
const ORDER_KINDS = periodKindsOf(["termMonth", "termThreeMonths", "termYear"]);

/** Where the comparison is made: the books list stands in for any ledger with a DATE filter. */
const ENTITY = "book";

// A live backend, and the first navigation to a route additionally waits for the dev server to compile
// it.
test.describe.configure({ timeout: 120_000 });

/**
 * "Jahr bis heute" in a list filter: a year that starts on the date the user gave and ends today, so one
 * click on `‹` is last year's same stretch — 01.11.2025–22.08.2026 against 01.11.2024–22.08.2025.
 *
 * The art travels with the filter entry (`MagicFilterEntry.Value.periodKind`), because these two dates
 * cannot say it: a range that happens to end today is no evidence it was meant to. That is what the
 * reload case is about — and why the end is recomputed when the stored filter comes back, which is the
 * difference to every other quick access, whose bounds are frozen the moment they are set.
 *
 * Nothing is spelled out: the field comes from `listMeta`, the texts from the account's catalog and every
 * expected date from the very functions the component computes with.
 */
test.describe("year to date", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await resetFilter(page, ENTITY);
  });

  test.afterEach(async ({ loggedInPage: page }) => {
    await resetFilter(page, ENTITY);
  });

  test("ends the year the begin opens on today", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    // A begin in the previous year, so "bis heute" is a stretch of months rather than the current month.
    const begin = startOfLastYear(context);
    await bound(page, format, field, "value").fill(format.date(begin));
    await pickKind(page, format, YEAR_TO_DATE);

    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(todayOf(context))
    );
    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(begin)
    );
  });

  test("compares with the same stretch a year back", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    const begin = startOfLastYear(context);
    await bound(page, format, field, "value").fill(format.date(begin));
    await pickKind(page, format, YEAR_TO_DATE);
    await page
      .getByRole("button", { name: t(YEAR_TO_DATE.tooltipPreviousKey) })
      .click();

    // Both ends a whole year back — the point of the art: the arrows move the period by a year, not by
    // the ~10 months it currently spans.
    const previous = YEAR_TO_DATE.shift(begin, -1, context);
    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(previous)
    );
    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(YEAR_TO_DATE.endOf(previous, context))
    );
  });

  test("keeps the art and moves the end on across a reload", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    const begin = startOfLastYear(context);
    await bound(page, format, field, "value").fill(format.date(begin));
    await pickKind(page, format, YEAR_TO_DATE);
    // Picking the art is the last edit; the list follows it on its own (no save click). Wait for the
    // request it settles into, which reflects the year-to-date state, not an intermediate one.
    const request = listRequest(page, ENTITY);

    // The art is sent as a third value beside the two dates: the backend stores it with the filter and
    // returns it untouched, and without it the reload below could only restore frozen bounds.
    const entry = (await request).entries.find(
      (candidate) => candidate.field === field.id
    );
    expect(entry?.value.periodKind).toBe(YEAR_TO_DATE.id);
    expect(entry?.value.from).toBe(begin);

    await page.reload();
    await reopenPill(page, t, field.label!);
    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(begin)
    );
    // Recomputed rather than restored: today, whichever day the list is opened on.
    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(todayOf(context))
    );
    // And still the art in effect, so the comparison is one click away again. The filter popover has room
    // and spells the art out (`longLabel`, see [PeriodQuickSelect]), so the trigger carries the long name.
    await expect(picker(page, format)).toHaveText(
      kindName(format, YEAR_TO_DATE)
    );
  });

  test("stands beside the lengths a ledger is read in", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, format.t, field.label!);

    // What a list of invoices is asked: which month, which quarter, which year — and the year up to today
    // for the comparison, as the entry below "Jahr". The order is [PERIOD_KINDS]', which is also what an
    // entered range is read as: the calendar month first, so 01.03.–31.03. keeps paging month by month
    // instead of being read as a term of three months.
    await picker(page, format).click();
    await expect(page.getByRole("option")).toHaveText(
      DEFAULT_KINDS.map((kind) => kindName(format, kind))
    );
  });

  test("is not offered where a list's dates are terms", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    await resetFilter(page, "order");
    const field = await filterField(
      page,
      "order",
      "DATE",
      "Does AuftragDao still index periodOfPerformanceBegin?"
    );
    await goto(page, "/order");
    await openPill(page, t, field.label!);

    // The order book asks about terms from a begin, and a book of commitments running into the future is
    // only confused by "bis heute" (see ORDER_PAGE.filterPeriodKinds).
    await picker(page, format).click();
    await expect(
      page.getByRole("option", {
        name: kindName(format, YEAR_TO_DATE),
        exact: true,
      })
    ).toHaveCount(0);
    // Exactly the three terms the page declares — and note that "Monat" alone would prove nothing here:
    // the calendar month and the month from the begin share the text (`calendar.month` = `duration.month`),
    // which is the whole reason the ids differ.
    await expect(page.getByRole("option")).toHaveText(
      ORDER_KINDS.map((kind) => kindName(format, kind))
    );
    await resetFilter(page, "order");
  });
});

/** The list's DATE filter field, whichever one that is (see [filterField]). */
function dateField(page: Page) {
  return filterField(
    page,
    ENTITY,
    "DATE",
    "Does BookDao still index lendOutDate?"
  );
}

/**
 * A begin in the previous year — the 1st of November, the fiscal year the case came from. Derived from
 * the account's own zone, because which year is "the previous" one depends on the day it is there.
 */
function startOfLastYear(context: UserFormat["context"]): string {
  return `${Number(todayOf(context).slice(0, 4)) - 1}-11-01`;
}
