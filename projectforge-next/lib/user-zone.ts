import type { FormatContext } from "./format";

/**
 * Converting between an instant on the wire and the wall clock the user reads it as.
 *
 * The wire format of a timestamp filter is an ISO string in UTC (`2026-08-09T08:12:34.000Z`) — the
 * one form `PFDateTimeUtils.parse` reads unambiguously. Without an offset it assumes UTC
 * (`defaultZoneId ?: ZoneOffset.UTC`), so a bare `2026-08-09T10:00` would silently land two hours
 * off for a German user. A date alone (`2026-08-09`) parses to `null` there and drops the bound.
 *
 * The wall clock, meanwhile, is the user's: `ctx.timeZone` from userData, not the browser's zone.
 * Those differ whenever the account is set to another zone than the machine, and then every
 * conversion here would be wrong by their difference — which is the whole reason this module exists
 * instead of `new Date(y, m, d, h, min)`.
 */

/** A wall clock reading: the ISO date and the time of day, both as the user sees them. */
export interface ZonedParts {
  /** `yyyy-MM-dd`, the format lib/date-parse.ts speaks. */
  date: string;
  /** `HH:mm`, 24-hour — the value of an `<input type="time">`, independent of locale. */
  time: string;
}

/** Time of day a bound falls back to when only a date was entered. */
export const DEFAULT_FROM_TIME = "00:00";
export const DEFAULT_TO_TIME = "23:59";

function partsFormatter(timeZone: string | undefined): Intl.DateTimeFormat {
  // "en-CA" writes an ISO-like date; the parts are read individually anyway, so only the calendar
  // and numbering system matter — never the user's locale, which must not reach the wire format.
  return new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

/**
 * The offset of `ctx.timeZone` at the given instant, in minutes east of UTC (Berlin in summer: 120).
 *
 * Read by formatting the instant in the zone and comparing that wall clock against UTC, because
 * `Intl` exposes no offset as a number. Doing it per instant is what makes DST work: the same zone
 * is +60 in January and +120 in July.
 */
export function offsetMinutesAt(date: Date, ctx: FormatContext): number {
  if (!ctx.timeZone) return -date.getTimezoneOffset();
  const parts = partsFormatter(ctx.timeZone).formatToParts(date);
  const at = (type: Intl.DateTimeFormatPartTypes) =>
    Number(parts.find((part) => part.type === type)?.value);
  // Hour 24 is how "en-CA" with hour12:false writes midnight; Date.UTC takes it as the next day,
  // which is the same instant, so it needs no special case.
  const asUtc = Date.UTC(
    at("year"),
    at("month") - 1,
    at("day"),
    at("hour"),
    at("minute")
  );
  // The wall clock read as if it were UTC, minus the real instant, is the offset. Rounded to the
  // minute: the instant may carry seconds the formatter dropped.
  return Math.round((asUtc - date.getTime()) / 60_000);
}

/** Splits an instant into the date and time of day the user sees it as. */
export function zonedPartsOf(
  iso: string | null | undefined,
  ctx: FormatContext
): ZonedParts | null {
  if (!iso) return null;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return null;
  const parts = partsFormatter(ctx.timeZone).formatToParts(date);
  const at = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((part) => part.type === type)?.value ?? "";
  const hour = at("hour") === "24" ? "00" : at("hour");
  return {
    date: `${at("year")}-${at("month")}-${at("day")}`,
    time: `${hour}:${at("minute")}`,
  };
}

/**
 * The instant a wall clock in the user's zone stands for, as an ISO string in UTC.
 *
 * The offset to subtract depends on the very instant being looked for, so it cannot simply be read:
 * each offset in effect around that day is tried, and a candidate counts only if the zone really
 * has that offset at the instant it produces. That is what gets the DST edges right, matching
 * `ZonedDateTime`:
 *
 * - **Clocks go forward** (Berlin 2026-03-29, 02:00 → 03:00): 02:30 never happens, no candidate is
 *   valid, and the wall clock is read with the offset in force *before* the jump — which lands it
 *   at 03:30 local, the same instant Java resolves it to.
 * - **Clocks go back** (Berlin 2026-10-25, 03:00 → 02:00): 02:30 happens twice, both candidates are
 *   valid, and the earlier one wins.
 */
export function zonedIsoOf(
  date: string | null | undefined,
  time: string | null | undefined,
  ctx: FormatContext,
  /**
   * Time of day for a date entered without one. The caller knows which bound it is filling — a
   * range's start means midnight, its end the last minute of the day — so it is not guessed here.
   */
  fallbackTime: string = DEFAULT_FROM_TIME
): string | null {
  if (!date) return null;
  const day = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  if (!day) return null;
  const clock = /^(\d{1,2}):(\d{2})/.exec(time?.trim() || fallbackTime);
  if (!clock) return null;

  const wall = Date.UTC(+day[1], +day[2] - 1, +day[3], +clock[1], +clock[2]);
  // Half a day either side of the wall clock: a transition is at most one per day, so this brackets
  // both offsets whenever the wall clock is near one, and yields a single offset when it is not.
  const offsets = [
    ...new Set(
      [-12, 0, 12].map((h) =>
        offsetMinutesAt(new Date(wall + h * 3_600_000), ctx)
      )
    ),
  ];

  const valid = offsets
    .map((offset) => wall - offset * 60_000)
    .filter(
      (instant, index) =>
        offsetMinutesAt(new Date(instant), ctx) === offsets[index]
    );
  // Ambiguous: the earlier of the two. Nonexistent: the offset before the jump, i.e. the smallest —
  // a forward jump always increases the offset, in either hemisphere.
  const instant = valid.length
    ? Math.min(...valid)
    : wall - Math.min(...offsets) * 60_000;
  return new Date(instant).toISOString();
}

/** Now, in the wire format. */
export function nowIso(): string {
  return new Date().toISOString();
}

/** Shifts an instant by whole minutes, for the quick-select periods. */
export function shiftIsoByMinutes(iso: string, minutes: number): string {
  return new Date(new Date(iso).getTime() + minutes * 60_000).toISOString();
}

/**
 * Midnight of the day `dayOffset` days from the one `iso` falls on, in the user's zone.
 *
 * Used by the "today" / "since yesterday" periods, which mean the user's midnight — not the
 * browser's, and not UTC's.
 */
export function startOfDayIso(
  iso: string,
  ctx: FormatContext,
  dayOffset = 0
): string | null {
  const parts = zonedPartsOf(iso, ctx);
  if (!parts) return null;
  const [year, month, day] = parts.date.split("-").map(Number);
  const shifted = new Date(Date.UTC(year, month - 1, day + dayOffset));
  const date = `${shifted.getUTCFullYear()}-${String(shifted.getUTCMonth() + 1).padStart(2, "0")}-${String(shifted.getUTCDate()).padStart(2, "0")}`;
  return zonedIsoOf(date, "00:00", ctx);
}
