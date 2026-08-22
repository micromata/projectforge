import { dateOf, daysBetweenDates, isoOf, shiftDateByDays } from "./date-parse";

/**
 * Lengths a date period can be given: pick "1 Monat" next to the two ends and the end follows the
 * begin, so only the start date has to be entered.
 *
 * Deliberately *not* ./date-period.ts. A period there snaps to whole calendar periods — the whole
 * month a date lies in, which is what a list filter wants. A duration here is a term measured off the
 * begin: `begin + duration - 1 day`, so 15.03. + 1 Monat ends on 14.04. and 01.03. on 31.03. That is
 * what a Leistungszeitraum on an order is, and why the two live side by side rather than one of them
 * growing a mode.
 *
 * A duration lives as the two `yyyy-MM-dd` strings it produces, like everything in ./date-parse.ts —
 * no `Date` is ever a value here, and no time zone enters: the length of a term is the same in every
 * one of them.
 *
 * The month arithmetic is `LocalDate.plusMonths(n).minusDays(1)`, clamping the day into the target
 * month — 31.01. + 1 Monat ends on 27.02. One rule, the backend's, and no special case for the last
 * day of a month: that would make 30.01. end on 27.02. and 31.01. a day later on 28.02., a jump
 * nobody can predict from a select. Whoever wants that date types it into the end box, which is what
 * dissolves the duration.
 */

export type DurationId = "week" | "month" | "threeMonths" | "year";

/**
 * One length offered.
 *
 * Every entry carries its text as a spelled-out `…Key` rather than letting a component build
 * `` t(`duration.${id}`) ``: a key assembled at runtime is invisible to `NextI18nKeyScanner`, so it
 * would never reach `messages/generated.*.json` and the select would show the raw key. Spelled out it
 * is found, and a typo is reported by the generator. Same reason as [PeriodUnit]'s keys.
 */
export interface Duration {
  id: DurationId;
  /** Whole months of the length; 0 for one counted in days. A year is twelve of them, one code path. */
  months: number;
  /** Whole days on top of [months]. */
  days: number;
  labelKey: string;
  /**
   * The same length in one or two characters ("3M"), for a picker that has to fit beside two date boxes.
   * The full [labelKey] is what the list of choices shows, so the short form never has to stand alone.
   */
  shortLabelKey: string;
  /**
   * Fills the `{arg0}` of a counted name ("3 Monate", "3M"); absent where the key names the unit alone.
   * The same count for both texts — they say the same thing.
   */
  labelArg?: number;
}

/**
 * Every duration there is, shortest first — the order a select offers them in.
 *
 * `threeMonths`, not `quarter`: a Quartal is January to March, while this is three months from
 * whenever the term begins. [PeriodUnitId] keeps `quarter` for the calendar meaning, and the two must
 * not be confused.
 */
export const DURATIONS: readonly Duration[] = [
  {
    id: "week",
    months: 0,
    days: 7,
    labelKey: "calendar.week",
    shortLabelKey: "duration.short.week",
  },
  {
    id: "month",
    months: 1,
    days: 0,
    labelKey: "calendar.month",
    shortLabelKey: "duration.short.month",
  },
  {
    id: "threeMonths",
    months: 3,
    days: 0,
    labelKey: "duration.months",
    shortLabelKey: "duration.short.months",
    labelArg: 3,
  },
  {
    id: "year",
    months: 12,
    days: 0,
    labelKey: "calendar.year",
    shortLabelKey: "duration.short.year",
  },
];

/** The ids of [DURATIONS], for a field that offers all of them. */
export const DURATION_IDS: readonly DurationId[] = DURATIONS.map((d) => d.id);

/** The durations named, in the order [DURATIONS] has them; an unknown id is dropped. */
export function durationsOf(
  ids: readonly DurationId[] | undefined
): Duration[] {
  if (!ids?.length) return [];
  return DURATIONS.filter((duration) => ids.includes(duration.id));
}

/** The one duration with that id, or null — for a selection held as an id. */
export function durationOf(id: DurationId | null | undefined): Duration | null {
  return DURATIONS.find((duration) => duration.id === id) ?? null;
}

/**
 * The same day of the month `months` months on, clamped into it — `LocalDate.plusMonths`.
 *
 * The day is set aside before the month moves, rather than `date.setMonth(+1)` on the 31st: that
 * would turn the 31st of January into the 3rd of March and a "month" would be five weeks long.
 * Nothing in ./date-period.ts answers this — `shiftMonths` returns the first of the month by
 * contract, `endOfMonth` is period-aligned.
 */
function plusMonths(
  iso: string | null | undefined,
  months: number
): string | null {
  const date = dateOf(iso);
  if (!date) return null;
  if (months === 0) return isoOf(date);
  const day = date.getDate();
  date.setDate(1);
  date.setMonth(date.getMonth() + months);
  const lastOfTarget = new Date(
    date.getFullYear(),
    date.getMonth() + 1,
    0
  ).getDate();
  date.setDate(Math.min(day, lastOfTarget));
  return isoOf(date);
}

/**
 * The last day of the term that begins on `begin` and lasts `duration`, or null when `begin` is no
 * date — a month starting on the 15th ends on the 14th.
 */
export function endOfDuration(
  begin: string | null | undefined,
  duration: Duration
): string | null {
  const shifted = plusMonths(begin, duration.months);
  if (!shifted) return null;
  return shiftDateByDays(shifted, duration.days - 1);
}

/**
 * The period `steps` of its own lengths on, as its two ends — or null when there is nothing to move
 * (an end missing, or one of them no date). This is what the paging arrows beside the two boxes do.
 *
 * One rule, two ways of measuring the length. With a duration in effect the length *is* the duration,
 * counted in whole months where it counts in months: from "1 Monat" on 01.03.–31.03. one click has to
 * reach 01.04.–30.04., not a period shifted by that month's 31 days. With none it is the number of days
 * the two ends span, so a period entered by hand moves by exactly as much as it covers.
 *
 * Not reversible at a month's end — 31.01. one on is 28.02., one back from there is 28.01. That is what
 * `LocalDate.plusMonths` does, and the clamping is already the rule of the duration itself (see
 * [plusMonths]); a stepper that remembered where it came from would show a period the two boxes do not.
 */
export function shiftBounds(
  from: string | null | undefined,
  to: string | null | undefined,
  duration: Duration | null,
  steps: number
): { from: string; to: string } | null {
  if (!dateOf(from) || !dateOf(to)) return null;
  if (duration) {
    const begin = shiftDateByDays(
      plusMonths(from, duration.months * steps),
      duration.days * steps
    );
    const end = endOfDuration(begin, duration);
    return begin && end ? { from: begin, to: end } : null;
  }
  // Inclusive of both ends: a single day is a length of one, so paging it lands on the next day rather
  // than staying put.
  const length = daysBetweenDates(from, to);
  if (length == null) return null;
  const begin = shiftDateByDays(from, (length + 1) * steps);
  const end = shiftDateByDays(to, (length + 1) * steps);
  return begin && end ? { from: begin, to: end } : null;
}

/**
 * Which duration the two bounds are, or null when they are none.
 *
 * Defined through [endOfDuration], so what is read back can never disagree with what was written,
 * whatever the arithmetic does at a month's end. Both ends are needed: a half-open range has no
 * length. The order of `durations` cannot matter — 7 against ~28–31 against ~89–92 against ~365 days,
 * so no two of them ever reach the same end from the same begin.
 */
export function durationOfBounds(
  from: string | null | undefined,
  to: string | null | undefined,
  durations: readonly Duration[]
): Duration | null {
  if (!from || !to) return null;
  return (
    durations.find((duration) => endOfDuration(from, duration) === to) ?? null
  );
}
