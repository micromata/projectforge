import { todayOf } from "./user-zone";
import {
  endOfMonth,
  endOfWeek,
  firstOfMonth,
  firstOfWeek,
  isEarlierInYear,
  isoOfParts,
  partsOf,
  plusDays,
  plusMonths,
  plusYears,
  withYear,
} from "./date-period-math";
import type { PeriodKind, PeriodKindId } from "./date-period";

/**
 * The kinds a period can be given as — the registry ./date-period.ts hands out.
 *
 * Three flavours, and they differ only in what their own `beginOf` and `endOf` do (see [PeriodKind]):
 *
 * - **calendar-aligned** (`month`): the begin snaps to the section's first day and the end is its last,
 *   so "Monat" is 01.03.–31.03. and is named "März 2026". That is what a list filter asks for — "welcher
 *   Monat?" — and what Wicket's `QuickSelectPanel` offers.
 * - **terms** (`term*`): a length measured off the begin as it was given, `begin + length - 1 day`, so
 *   "Monat" from the 15th ends on the 14th. That is what a Leistungszeitraum is.
 * - **until today** (`yearToDate`): the begin as it was given, the end today, paged in whole years. The
 *   business year up to now, against the same window a year back.
 *
 * The two meanings of "Monat" are both right, each for its own surface, which is why they are two kinds
 * rather than one with a switch: a field offers the ids that make sense there, and nothing has to be
 * told which flavour it is dealing with.
 */

/**
 * Calendar-aligned week: the begin snaps to the user's own first weekday (`weekStartsOn`, Monday
 * where unset), the end is six days on, and one click pages a whole week. That is what a list filter
 * asks for — "welche Woche?" — and it lines up with the list's KW column, unlike the `termWeek` term
 * (rolling seven days off the begin) which is a Leistungszeitraum. Wicket's `QuickSelectWeekPanel`.
 */
const WEEK: PeriodKind = {
  id: "week",
  labelKey: "calendar.week",
  shortLabelKey: "duration.short.week",
  tooltipPreviousKey: "calendar.quickselect.tooltip.selectPreviousWeek",
  tooltipCurrentKey: "calendar.quickselect.tooltip.selectCurrentWeek",
  tooltipNextKey: "calendar.quickselect.tooltip.selectNextWeek",
  beginOf: (iso, ctx) => firstOfWeek(iso, ctx.weekStartsOn),
  endOf: (iso, ctx) => endOfWeek(iso, ctx.weekStartsOn),
  shift: (iso, steps, ctx) => firstOfWeek(iso, ctx.weekStartsOn, steps),
};

const MONTH: PeriodKind = {
  id: "month",
  labelKey: "calendar.month",
  shortLabelKey: "duration.short.month",
  tooltipPreviousKey: "calendar.quickselect.tooltip.selectPreviousMonth",
  tooltipCurrentKey: "calendar.quickselect.tooltip.selectCurrentMonth",
  tooltipNextKey: "calendar.quickselect.tooltip.selectNextMonth",
  beginOf: (iso) => firstOfMonth(iso),
  endOf: (iso) => endOfMonth(iso),
  shift: (iso, steps) => firstOfMonth(iso, steps),
};

/**
 * From the day the user entered up to today, paged in whole years.
 *
 * The anchor is **not** snapped: whatever begin stands in the two boxes is where the year starts, so a
 * business year needs no configuration anywhere — 01.11. entered means 01.11., and the backend (whose
 * `getBeginOfYear` and `LocalDatePeriod.wholeYear()` are hard-wired to January) never has to know.
 *
 * The end is today's day and month in whichever year lands inside `[anchor, anchor + 1 year)`. One click
 * on the arrow therefore only moves the anchor a year, and the end follows by itself: 01.11.2025–heute
 * becomes 01.11.2024–heute-vor-einem-Jahr, which is the comparison this kind exists for. Nothing has to
 * be remembered for that, and there is no second rule for "the current one".
 */
const YEAR_TO_DATE: PeriodKind = {
  id: "yearToDate",
  labelKey: "calendar.yearToDate",
  shortLabelKey: "calendar.short.yearToDate",
  tooltipPreviousKey: "calendar.quickselect.tooltip.selectPreviousYearToDate",
  tooltipCurrentKey: "calendar.quickselect.tooltip.selectCurrentYearToDate",
  tooltipNextKey: "calendar.quickselect.tooltip.selectNextYearToDate",
  // Its end moves with the calendar, so a pair of dates can never prove this kind was meant — it is
  // stored instead (see `periodOfBounds` and MagicFilterEntry.Value.periodKind).
  dependsOnToday: true,
  beginOf: (iso) => {
    const { year, month, day } = partsOf(iso);
    return isoOfParts(year, month, day);
  },
  endOf: (iso, ctx) => {
    const today = todayOf(ctx);
    const { year } = partsOf(iso);
    return withYear(today, isEarlierInYear(today, iso) ? year + 1 : year);
  },
  shift: (iso, steps) => plusYears(iso, steps),
  // With both boxes empty there is no anchor to take, and the calendar year up to today is the neutral
  // reading of the name — the one thing that needs no business year to be known.
  currentAnchor: (ctx) => isoOfParts(partsOf(todayOf(ctx)).year, 1, 1),
};

/**
 * One term: `begin + months + days - 1`, the begin left where it is.
 *
 * `termThreeMonths`, not "quarter": a Quartal is January to March, while this is three months from
 * wherever the term begins. Paging moves the begin by the same length, so the period stays the term it
 * is — not reversible at a month's end (31.01. one on is 28.02., one back from there 28.01.), which is
 * what `LocalDate.plusMonths` does and therefore what the backend stored a moment earlier.
 */
function term(
  id: PeriodKindId,
  labelKey: string,
  shortLabelKey: string,
  { months = 0, days = 0, labelArg }: TermLength
): PeriodKind {
  return {
    id,
    labelKey,
    shortLabelKey,
    labelArg,
    // No "current term": "die aktuelle Woche" is nothing one does to an agreed period of performance,
    // so the stepper offers no such button there (see [PeriodStepper]).
    tooltipPreviousKey: "duration.previous",
    tooltipNextKey: "duration.next",
    beginOf: (iso) => {
      const { year, month, day } = partsOf(iso);
      return isoOfParts(year, month, day);
    },
    endOf: (iso) => plusDays(plusMonths(iso, months), days - 1),
    shift: (iso, steps) =>
      plusDays(plusMonths(iso, months * steps), days * steps),
  };
}

interface TermLength {
  /** Whole months of the length; a year is twelve of them, one code path. */
  months?: number;
  /** Whole days on top of the months. */
  days?: number;
  /** Fills the `{arg0}` of a counted name ("3 Monate"), where the key is one. */
  labelArg?: number;
}

/** The terms, shortest first — the order a picker offers them in. */
export const TERM_KINDS: readonly PeriodKind[] = [
  term("termWeek", "calendar.week", "duration.short.week", { days: 7 }),
  term("termMonth", "calendar.month", "duration.short.month", { months: 1 }),
  term("termThreeMonths", "duration.months", "duration.short.months", {
    months: 3,
    labelArg: 3,
  }),
  term("termYear", "calendar.year", "duration.short.year", { months: 12 }),
];

/**
 * Every kind there is, and this is the order every picker offers its own selection in (see
 * [periodKindsOf]): the calendar week and month, then the terms rising in length, and "Jahr bis heute"
 * last — it is the year again, only ending today, so it reads as the entry below "Jahr" rather than
 * between the lengths.
 */
export const PERIOD_KINDS: readonly PeriodKind[] = [
  WEEK,
  MONTH,
  ...TERM_KINDS,
  YEAR_TO_DATE,
];
