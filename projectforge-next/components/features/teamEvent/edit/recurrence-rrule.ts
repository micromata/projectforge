import { Frequency, RRule, type Options } from "rrule";

/**
 * The RFC 5545 recurrence rule of a team event, reduced to the four things the form edits, and the pure
 * translation between it and the `recurrenceRule` string the backend stores.
 *
 * The backend keeps the rule as a bare `RRULE` body ("FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE"), the `RRULE:`
 * prefix stripped (`TeamEventDO.recurrenceRule` setter). The `rrule` package does the actual RFC parsing
 * and formatting; this module is only the mapping to a small, UI-shaped model, so the section component
 * stays declarative and this stays testable without React.
 */

/** The frequencies the form offers — the legacy `RecurrenceFrequency` without `HOURLY`, and no `NONE`. */
export type RecurrenceFreq = "YEARLY" | "MONTHLY" | "WEEKLY" | "DAILY";

/** iCalendar weekday tokens, Monday first, as `BYDAY` writes them and the weekday checkboxes offer them. */
export const WEEKDAYS = ["MO", "TU", "WE", "TH", "FR", "SA", "SU"] as const;
export type Weekday = (typeof WEEKDAYS)[number];

/**
 * The rule as the form holds it. `freq` null means "no recurrence" (an empty rule); `byWeekday` only
 * matters while `freq` is `WEEKLY`; `until`, when set, is an inclusive last day as `yyyy-MM-dd`.
 */
export interface RecurrenceModel {
  freq: RecurrenceFreq | null;
  interval: number;
  byWeekday: Weekday[];
  until: string | null;
}

/**
 * The top-level options the recurrence select offers: no recurrence, one of the four plain frequencies
 * (a bare `FREQ` repeated every period), or "customized" — the interval, weekdays and end date panel.
 */
export type RecurrenceMode = "NONE" | RecurrenceFreq | "CUSTOMIZED";

/**
 * Which top-level option a stored rule presents as, mirroring the legacy `CalendarEventRecurrence`: a
 * bare frequency with interval 1 and nothing else is that plain frequency; anything carrying a non-1
 * interval, weekdays or an end date is "customized"; no frequency at all is "none". This only seeds the
 * select on load — once the user picks "customized" the section holds that choice itself, so a customized
 * rule that happens to read back as a plain frequency (interval 1, no extras) does not snap the select.
 */
export function recurrenceMode(model: RecurrenceModel): RecurrenceMode {
  if (!model.freq) return "NONE";
  if (model.interval !== 1 || model.byWeekday.length > 0 || model.until != null)
    return "CUSTOMIZED";
  return model.freq;
}

const FREQ_TO_RRULE: Record<RecurrenceFreq, Frequency> = {
  YEARLY: Frequency.YEARLY,
  MONTHLY: Frequency.MONTHLY,
  WEEKLY: Frequency.WEEKLY,
  DAILY: Frequency.DAILY,
};

const RRULE_TO_FREQ: Partial<Record<Frequency, RecurrenceFreq>> = {
  [Frequency.YEARLY]: "YEARLY",
  [Frequency.MONTHLY]: "MONTHLY",
  [Frequency.WEEKLY]: "WEEKLY",
  [Frequency.DAILY]: "DAILY",
};

/** A rule with no recurrence — the starting point and what an unparseable or non-recurring rule reads as. */
export function emptyRecurrence(): RecurrenceModel {
  return { freq: null, interval: 1, byWeekday: [], until: null };
}

/**
 * Reads a stored `recurrenceRule` into the model. Anything without a `FREQ` (an empty string, a rule the
 * form does not cover such as `HOURLY`) reads as "no recurrence" rather than throwing, so a value the UI
 * cannot represent is simply shown as none — never silently mangled on the way back out.
 */
export function parseRecurrence(
  rule: string | null | undefined
): RecurrenceModel {
  const cleaned = (rule ?? "").replace(/^RRULE:/i, "").trim();
  if (!cleaned || !/FREQ=/i.test(cleaned)) return emptyRecurrence();
  let opts: Partial<Options>;
  try {
    opts = RRule.parseString(cleaned);
  } catch {
    return emptyRecurrence();
  }
  const freq = opts.freq != null ? RRULE_TO_FREQ[opts.freq] : undefined;
  if (!freq) return emptyRecurrence();
  return {
    freq,
    interval: opts.interval && opts.interval > 0 ? opts.interval : 1,
    byWeekday: readWeekdays(opts.byweekday ?? null),
    until: opts.until ? toDateString(opts.until) : null,
  };
}

/**
 * Writes the model back to a `recurrenceRule` body (no `RRULE:` prefix, as the backend stores it), or the
 * empty string for no recurrence. `INTERVAL` is always present, as the legacy form wrote it; `BYDAY` only
 * for a weekly rule with days picked; `UNTIL` is the end of the chosen day in UTC, matching the day the
 * backend derives for `recurrenceUntil` (`TeamEventDO.fixUntilInRecur`).
 */
export function serializeRecurrence(model: RecurrenceModel): string {
  if (!model.freq) return "";
  const options: Partial<Options> = {
    freq: FREQ_TO_RRULE[model.freq],
    interval: model.interval > 1 ? model.interval : 1,
  };
  if (model.freq === "WEEKLY" && model.byWeekday.length > 0) {
    options.byweekday = model.byWeekday.map((day) => WEEKDAYS.indexOf(day));
  }
  if (model.until) options.until = untilDate(model.until);
  return RRule.optionsToString(options)
    .replace(/^RRULE:/i, "")
    .trim();
}

/** The end-of-day UTC instant a `recurrenceUntil` should carry for the chosen last day, or null. */
export function untilInstant(until: string | null): string | null {
  return until ? untilDate(until).toISOString() : null;
}

/** Normalises `rrule`'s several byweekday shapes (number, Weekday, arrays of either) to our tokens. */
function readWeekdays(byweekday: Options["byweekday"]): Weekday[] {
  if (byweekday == null) return [];
  const list = Array.isArray(byweekday) ? byweekday : [byweekday];
  const days = list
    .map((entry) =>
      typeof entry === "number" ? entry : (entry as { weekday: number }).weekday
    )
    .filter((n) => n >= 0 && n < WEEKDAYS.length)
    .map((n) => WEEKDAYS[n]);
  // Keep the calendar order and drop any duplicate a malformed rule might carry.
  return WEEKDAYS.filter((day) => days.includes(day));
}

/** The UTC calendar date of an instant as `yyyy-MM-dd` — `UNTIL` is read back as the day it names. */
function toDateString(date: Date): string {
  return date.toISOString().slice(0, 10);
}

/** The last inclusive second of the given `yyyy-MM-dd` in UTC, so the whole day stays in the series. */
function untilDate(day: string): Date {
  const [y, m, d] = day.split("-").map(Number);
  return new Date(Date.UTC(y, m - 1, d, 23, 59, 59));
}
