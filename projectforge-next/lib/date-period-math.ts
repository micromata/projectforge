/**
 * The ISO date arithmetic every period kind is built from (see ./date-period-kinds.ts).
 *
 * A date lives as `yyyy-MM-dd`, as everywhere since ./date-parse.ts, and no time zone enters here: the
 * first of a month and the length of a term are the same in every one of them. Which day *today* is
 * does depend on the zone, and that is why it is not here but in ./user-zone.ts (`todayOf`).
 *
 * Everything throws on something that is not a date rather than returning null. A kind computes with a
 * date it was handed; the callers that may hold half-typed text — a filter input, a date box being
 * filled in — catch it in the one place they already do (see `periodOfBounds`).
 *
 * The month arithmetic is `LocalDate.plusMonths`: the day is set aside before the month moves and then
 * clamped into it, so 31.01. plus one month is 28.02. and not the 3rd of March, which `setMonth(+1)`
 * would give. The backend computes a period of performance the same way.
 */

export interface DateParts {
  year: number;
  month: number;
  day: number;
}

/** The parts of an ISO date, as numbers. */
export function partsOf(iso: string): DateParts {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  if (!match) throw new Error(`Not an ISO date: ${iso}`);
  return { year: +match[1], month: +match[2], day: +match[3] };
}

export function isoOfParts(year: number, month: number, day: number): string {
  return `${String(year).padStart(4, "0")}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

/**
 * How many days that month has — read as day 0 of the *following* month, which is the last of this
 * one. That is what knows about the short months and about February in a leap year.
 */
export function daysOfMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

/** ±n days. */
export function plusDays(iso: string, days: number): string {
  const { year, month, day } = partsOf(iso);
  const date = new Date(year, month - 1, day + days);
  return isoOfParts(date.getFullYear(), date.getMonth() + 1, date.getDate());
}

/** The same day of the month `months` months on, clamped into it — `LocalDate.plusMonths`. */
export function plusMonths(iso: string, months: number): string {
  const { year, month, day } = partsOf(iso);
  if (months === 0) return isoOfParts(year, month, day);
  const total = year * 12 + (month - 1) + months;
  const targetYear = Math.floor(total / 12);
  // Modulo that stays positive, so a date before the year 0 does not land in month 0.
  const targetMonth = (((total % 12) + 12) % 12) + 1;
  return isoOfParts(
    targetYear,
    targetMonth,
    Math.min(day, daysOfMonth(targetYear, targetMonth))
  );
}

/** Whole years on, clamped the same way: 29.02. one year on is 28.02. */
export function plusYears(iso: string, years: number): string {
  return plusMonths(iso, years * 12);
}

/** The same day and month in another year, clamped — 29.02. in a common year is the 28th. */
export function withYear(iso: string, year: number): string {
  const { month, day } = partsOf(iso);
  return isoOfParts(year, month, Math.min(day, daysOfMonth(year, month)));
}

/** First day of the month `steps` months from the one `iso` lies in. */
export function firstOfMonth(iso: string, steps = 0): string {
  const { year, month } = partsOf(iso);
  const total = year * 12 + (month - 1) + steps;
  return isoOfParts(Math.floor(total / 12), (((total % 12) + 12) % 12) + 1, 1);
}

/** Last day of the month `iso` lies in. */
export function endOfMonth(iso: string): string {
  const { year, month } = partsOf(iso);
  return isoOfParts(year, month, daysOfMonth(year, month));
}

/**
 * First day of the week `steps` weeks from the one `iso` lies in, aligned to the user's own first
 * weekday (`weekStartsOn`, 0 = Sunday … 6 = Saturday — the react-day-picker form, see FormatContext).
 *
 * Defaults to Monday when the setting is absent: the German ISO calendar week is Monday-based and
 * matches the list's KW column, the same fallback the date picker takes (see date-input-calendar.tsx).
 */
export function firstOfWeek(
  iso: string,
  weekStartsOn: number | undefined,
  steps = 0
): string {
  const start = weekStartsOn ?? 1;
  const { year, month, day } = partsOf(iso);
  const weekday = new Date(year, month - 1, day).getDay();
  const offset = (((weekday - start) % 7) + 7) % 7;
  return plusDays(iso, -offset + steps * 7);
}

/** Last day (begin + 6) of the week `iso` lies in, aligned the same way. */
export function endOfWeek(
  iso: string,
  weekStartsOn: number | undefined
): string {
  return plusDays(firstOfWeek(iso, weekStartsOn), 6);
}

/**
 * Whether the day and month of `iso` come before those of `other`, the year ignored — the question
 * "is today still inside the year that began on the anchor's day?" (see the `yearToDate` kind).
 */
export function isEarlierInYear(iso: string, other: string): boolean {
  const a = partsOf(iso);
  const b = partsOf(other);
  return a.month !== b.month ? a.month < b.month : a.day < b.day;
}

/**
 * How many days the two ends span, both counted: a single day is a length of one, so paging a period
 * by its span lands on the next day rather than staying put.
 *
 * Rounded because two local midnights are 23 or 25 hours apart across a DST switch.
 */
export function inclusiveDays(from: string, to: string): number {
  const begin = partsOf(from);
  const end = partsOf(to);
  const millis =
    new Date(end.year, end.month - 1, end.day).getTime() -
    new Date(begin.year, begin.month - 1, begin.day).getTime();
  return Math.round(millis / 86_400_000) + 1;
}
