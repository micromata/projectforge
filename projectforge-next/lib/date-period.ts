import { formatMonthYear, type FormatContext } from "./format";
import { todayIso } from "./date-parse";
import { nowIso, zonedPartsOf } from "./user-zone";

/**
 * Calendar-aligned periods: the whole month a date lies in, the one before it, the one after.
 *
 * This is what Wicket's `QuickSelectPanel` offers next to a date period — one click sets *both* ends
 * of the period at once, and the arrows page it. Not to be confused with
 * `components/data-table/history-interval-presets.ts`: those are rolling windows that always end at
 * "now" ("die letzten 30 Tage"), while a period here begins and ends on a calendar boundary.
 *
 * A period lives as two `yyyy-MM-dd` strings, like everything in ./date-parse.ts — that is how a
 * `LocalDate` travels over the wire, and no `Date` is ever a value here. What a time zone does to
 * this belongs in ./date-period-instant.ts; the arithmetic below is deliberately zone-free.
 *
 * Only the month is offered today. Week, quarter and year are the units Wicket had or would plausibly
 * want next, and adding one means adding a [PeriodUnit] to [PERIOD_UNITS] — the components take the
 * list, never a unit by name. Two things to know before doing so:
 *
 * - **week** needs `ctx.weekStartsOn` (the user's setting, not the locale's), and its *number* ("KW
 *   34") follows `WeekFields(firstDayOfWeek, minimalDaysInFirstWeek)` in the backend (see
 *   `PFDay.getWeekOfYear`). `minimalDaysInFirstWeek` is not in userData, so a hand-rolled week number
 *   would be a second, subtly different definition — use date-fns `getWeek` there.
 * - **quarter** cannot be requested by the backend at all: `UIFilterTimestampElement.QuickSelector`
 *   has YEAR, MONTH, WEEK and DAY, but no QUARTER.
 */

export type PeriodUnitId = "week" | "month" | "quarter" | "year";

/**
 * One granularity a period can be paged in.
 *
 * Every unit carries its own texts as keys rather than letting a component build them
 * (`` t(`…select${unit}`) ``): a key assembled at runtime is invisible to `NextI18nKeyScanner`, so it
 * would never reach `messages/generated.*.json` and the button would show the raw key. Spelled out as
 * a `…Key` property it is found, and a typo is reported by the generator.
 */
export interface PeriodUnit {
  id: PeriodUnitId;
  /** Name of the unit itself, for a picker that offers more than one. */
  labelKey: string;
  tooltipPreviousKey: string;
  tooltipCurrentKey: string;
  tooltipNextKey: string;
  /** First day of the period `iso` falls in. */
  beginOf(iso: string, ctx: FormatContext): string;
  /** Last day of it. */
  endOf(iso: string, ctx: FormatContext): string;
  /** The period `steps` units away, as its first day. */
  shift(iso: string, steps: number, ctx: FormatContext): string;
  /** How the period is named to the user, e.g. "August 2026". */
  label(iso: string, ctx: FormatContext): string;
}

/** The parts of an ISO date, as numbers. */
function partsOf(iso: string): { year: number; month: number; day: number } {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  if (!match) throw new Error(`Not an ISO date: ${iso}`);
  return { year: +match[1], month: +match[2], day: +match[3] };
}

function isoOfParts(year: number, month: number, day: number): string {
  return `${String(year).padStart(4, "0")}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

/**
 * The first of the month `steps` months from the one `iso` lies in.
 *
 * Counts in whole months and only then picks a day, rather than `date.setMonth(+1)`: the latter turns
 * the 31st of January into the 3rd of March, so paging a period that happens to start on a long
 * month's last day would skip a month.
 */
function shiftMonths(iso: string, steps: number): string {
  const { year, month } = partsOf(iso);
  const total = year * 12 + (month - 1) + steps;
  return isoOfParts(Math.floor(total / 12), (total % 12) + 1, 1);
}

/**
 * Last day of the month `iso` lies in.
 *
 * Its length comes from day 0 of the *following* month, which is the last of this one — that is what
 * knows about the short months and about February in a leap year.
 */
function endOfMonth(iso: string): string {
  const { year, month } = partsOf(iso);
  return isoOfParts(year, month, new Date(year, month, 0).getDate());
}

const MONTH: PeriodUnit = {
  id: "month",
  labelKey: "calendar.month",
  tooltipPreviousKey: "calendar.quickselect.tooltip.selectPreviousMonth",
  tooltipCurrentKey: "calendar.quickselect.tooltip.selectCurrentMonth",
  tooltipNextKey: "calendar.quickselect.tooltip.selectNextMonth",
  beginOf: (iso) => shiftMonths(iso, 0),
  endOf: (iso) => endOfMonth(iso),
  shift: (iso, steps) => shiftMonths(iso, steps),
  label: (iso, ctx) => formatMonthYear(iso, ctx),
};

/** Every unit there is, coarsest last — the order a picker would offer them in. */
export const PERIOD_UNITS: readonly PeriodUnit[] = [MONTH];

/** The units named, in the order [PERIOD_UNITS] has them; unknown ids are dropped. */
export function periodUnitsOf(
  ids: readonly PeriodUnitId[] | undefined
): PeriodUnit[] {
  if (!ids?.length) return [];
  return PERIOD_UNITS.filter((unit) => ids.includes(unit.id));
}

/** A whole period as the two dates it spans. */
export function boundsOfPeriod(
  unit: PeriodUnit,
  anchor: string,
  ctx: FormatContext
): { from: string; to: string } {
  return { from: unit.beginOf(anchor, ctx), to: unit.endOf(anchor, ctx) };
}

/** A period in effect: which unit it is paged in, and the day its current one begins on. */
export interface Period {
  unit: PeriodUnit;
  anchor: string;
}

/**
 * Which whole period the two bounds are, or null when they are not one.
 *
 * Both ends are needed — a half-open range is not a period, however it is written. The units are
 * checked in the given order, but the answer cannot depend on it: no whole month is also a whole
 * week, quarter or year, so at most one unit can ever match.
 */
export function periodOfBounds(
  from: string | null | undefined,
  to: string | null | undefined,
  units: readonly PeriodUnit[],
  ctx: FormatContext
): Period | null {
  if (!from || !to) return null;
  for (const unit of units) {
    let bounds;
    try {
      bounds = boundsOfPeriod(unit, from, ctx);
    } catch {
      // Not an ISO date, so not a period either — a value typed into a filter can be anything.
      return null;
    }
    if (bounds.from === from && bounds.to === to) {
      return { unit, anchor: bounds.from };
    }
  }
  return null;
}

/**
 * The period the arrows should page from, given bounds that are not a whole period — as its first
 * day, or null when neither bound is a date.
 *
 * The lower bound decides, and only in its absence the upper one: a range being filled in reads from
 * left to right, so the month the user just entered is the one the panel has to name. Paging from
 * "today" while a start date says otherwise would jump the range somewhere unrelated with one click.
 */
export function anchorOfBounds(
  // Undefined where quick access is switched off (`units[0]` of an empty list), which is a call the
  // caller shouldn't have to guard — the stepper renders nothing there anyway.
  unit: PeriodUnit | undefined,
  from: string | null | undefined,
  to: string | null | undefined,
  ctx: FormatContext
): string | null {
  if (!unit) return null;
  for (const bound of [from, to]) {
    if (!bound) continue;
    try {
      return unit.beginOf(bound, ctx);
    } catch {
      // Not a date but something half-typed into a filter; the other bound may still be one.
    }
  }
  return null;
}

/**
 * First day of the period today falls in.
 *
 * "Today" is the user's, read from `ctx.timeZone` — near midnight an account set to another zone than
 * the machine is on a different day, and then "aktueller Monat" could be the wrong one. Falls back to
 * the browser's day only when userData carries no zone.
 */
export function currentAnchorOf(unit: PeriodUnit, ctx: FormatContext): string {
  const today = zonedPartsOf(nowIso(), ctx)?.date ?? todayIso();
  return unit.beginOf(today, ctx);
}
