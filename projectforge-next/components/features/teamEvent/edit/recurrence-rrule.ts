import { Frequency, RRule, type Options } from "rrule";
import {
  emptyRecurrence,
  indicesToOnTheDay,
  onTheDayToIndices,
  WEEKDAYS,
  type RecurrenceFreq,
  type RecurrenceMode,
  type RecurrenceModel,
  type SetPos,
} from "./recurrence-model";

/**
 * The translation between the stored `recurrenceRule` string and the customized editor's model. The
 * backend keeps the rule as a bare `RRULE` body ("FREQ=MONTHLY;INTERVAL=1;BYSETPOS=1;BYDAY=MO"), the
 * `RRULE:` prefix stripped (`TeamEventDO.recurrenceRule` setter). The `rrule` package does the RFC parsing
 * and formatting; this is only the mapping to the small model, matching the legacy `compute*.js` exactly,
 * so the section components stay declarative and this stays testable without React.
 */

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

/**
 * Reads a stored `recurrenceRule` into the model. A rule without a `FREQ`, one the form does not cover
 * (`HOURLY`) or an unparseable one reads as "no recurrence" rather than throwing, so a value the UI cannot
 * represent is shown as none — never silently mangled on the way back out.
 */
export function parseRecurrence(
  rule: string | null | undefined
): RecurrenceModel {
  const opts = parseOptions(rule);
  const freq = opts && opts.freq != null ? RRULE_TO_FREQ[opts.freq] : undefined;
  if (!opts || !freq) return emptyRecurrence();

  const model = emptyRecurrence();
  model.freq = freq;
  model.interval = opts.interval && opts.interval > 0 ? opts.interval : 1;

  if (opts.count != null && opts.count > 0) {
    model.endMode = "COUNT";
    model.count = opts.count;
  } else if (opts.until) {
    model.endMode = "UNTIL";
    model.until = toDateString(opts.until);
  }

  const bymonth = firstNumber(opts.bymonth);
  const bymonthday = firstNumber(opts.bymonthday);
  const bysetpos = firstNumber(opts.bysetpos);
  const days = weekdayIndices(opts.byweekday);

  if (freq === "WEEKLY") {
    model.weeklyDays = WEEKDAYS.filter((_, i) => days.includes(i));
  } else if (freq === "MONTHLY") {
    if (bysetpos != null) {
      model.monthlyMode = "ONTHE";
      model.which = normalizeSetPos(bysetpos);
      model.onTheDay = indicesToOnTheDay(days);
    } else if (bymonthday != null) {
      model.monthlyDay = bymonthday;
    }
  } else if (freq === "YEARLY") {
    if (bymonth != null) model.yearlyMonth = bymonth;
    if (bysetpos != null) {
      model.yearlyMode = "ONTHE";
      model.which = normalizeSetPos(bysetpos);
      model.onTheDay = indicesToOnTheDay(days);
    } else if (bymonthday != null) {
      model.yearlyDay = bymonthday;
    }
  }
  return model;
}

/**
 * Writes the model back to a `recurrenceRule` body (no `RRULE:` prefix, as the backend stores it), or the
 * empty string for no recurrence — mirroring the legacy `compute*.js`. Interval is written for
 * monthly/weekly/daily (never yearly, which repeats every year); the `BY…`/`COUNT`/`UNTIL` fields follow
 * the active mode. `UNTIL` is the end of the chosen day in UTC, the day `TeamEventDO.fixUntilInRecur`
 * derives for `recurrenceUntil`.
 */
export function serializeRecurrence(model: RecurrenceModel): string {
  if (!model.freq) return "";
  const options: Partial<Options> = { freq: FREQ_TO_RRULE[model.freq] };
  const interval = model.interval > 1 ? model.interval : 1;

  if (model.freq === "YEARLY") {
    if (model.yearlyMode === "ON") {
      options.bymonth = model.yearlyMonth;
      options.bymonthday = model.yearlyDay;
    } else {
      options.bysetpos = model.which;
      options.byweekday = onTheDayToIndices(model.onTheDay);
      options.bymonth = model.yearlyMonth;
    }
  } else if (model.freq === "MONTHLY") {
    options.interval = interval;
    if (model.monthlyMode === "ON") {
      options.bymonthday = model.monthlyDay;
    } else {
      options.bysetpos = model.which;
      options.byweekday = onTheDayToIndices(model.onTheDay);
    }
  } else if (model.freq === "WEEKLY") {
    options.interval = interval;
    if (model.weeklyDays.length > 0)
      options.byweekday = model.weeklyDays.map((d) => WEEKDAYS.indexOf(d));
  } else {
    options.interval = interval;
  }

  if (model.endMode === "COUNT")
    options.count = model.count > 0 ? model.count : 1;
  else if (model.endMode === "UNTIL" && model.until)
    options.until = untilDate(model.until);

  return RRule.optionsToString(options)
    .replace(/^RRULE:/i, "")
    .trim();
}

/**
 * Which top-level option a stored rule seeds the select with: none for no `FREQ`; customized once any
 * `BY…`/`COUNT`/`UNTIL` or a non-1 interval is present; otherwise the plain frequency (a bare
 * `FREQ=…;INTERVAL=1`). Only seeds on load — the section then holds the choice itself, so a customized
 * rule that happens to read back as a plain frequency does not snap the select.
 */
export function recurrenceMode(
  rule: string | null | undefined
): RecurrenceMode {
  const opts = parseOptions(rule);
  const freq = opts && opts.freq != null ? RRULE_TO_FREQ[opts.freq] : undefined;
  if (!opts || !freq) return "NONE";
  const customized =
    (opts.interval != null && opts.interval !== 1) ||
    opts.bymonth != null ||
    opts.bymonthday != null ||
    opts.bysetpos != null ||
    opts.byweekday != null ||
    opts.count != null ||
    opts.until != null;
  return customized ? "CUSTOMIZED" : freq;
}

/** The end-of-day UTC instant a `recurrenceUntil` should carry for the chosen last day, or null. */
export function untilInstant(until: string | null): string | null {
  return until ? untilDate(until).toISOString() : null;
}

/** Parses a rule body (tolerating the `RRULE:` prefix), or null for empty/FREQ-less/unparseable input. */
function parseOptions(
  rule: string | null | undefined
): Partial<Options> | null {
  const cleaned = (rule ?? "").replace(/^RRULE:/i, "").trim();
  if (!cleaned || !/FREQ=/i.test(cleaned)) return null;
  try {
    return RRule.parseString(cleaned);
  } catch {
    return null;
  }
}

/** The first value of one of rrule's number|number[] fields, or null. */
function firstNumber(
  value: number | number[] | null | undefined
): number | null {
  if (value == null) return null;
  return Array.isArray(value) ? (value.length ? value[0] : null) : value;
}

/** Normalises rrule's several byweekday shapes (number, Weekday, arrays of either) to indices 0..6. */
function weekdayIndices(byweekday: Options["byweekday"] | undefined): number[] {
  if (byweekday == null) return [];
  const list = Array.isArray(byweekday) ? byweekday : [byweekday];
  return list
    .map((entry) =>
      typeof entry === "number" ? entry : (entry as { weekday: number }).weekday
    )
    .filter((n) => n >= 0 && n < WEEKDAYS.length);
}

/** Clamps an arbitrary `BYSETPOS` to the five the UI offers: first..fourth, or last. */
function normalizeSetPos(pos: number): SetPos {
  if (pos < 0) return -1;
  if (pos >= 4) return 4;
  return (pos <= 1 ? 1 : pos) as SetPos;
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
