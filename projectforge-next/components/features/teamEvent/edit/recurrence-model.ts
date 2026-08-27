/**
 * The UI-shaped recurrence model — the full set of things the customized editor edits, mirroring the
 * legacy `react-rrule-generator` state (see its `configureInitialState.js` and `computeRRule/toString/`).
 * The translation to and from the stored `recurrenceRule` string lives in recurrence-rrule.ts; this file
 * is only the shape, its defaults and the pure weekday-group mapping both sides share.
 */

/** The frequencies the form offers — the legacy `RecurrenceFrequency` without `HOURLY`, and no `NONE`. */
export type RecurrenceFreq = "YEARLY" | "MONTHLY" | "WEEKLY" | "DAILY";

/** iCalendar weekday tokens, Monday first, as `BYDAY` writes them and index 0=MO..6=SU (matches rrule). */
export const WEEKDAYS = ["MO", "TU", "WE", "TH", "FR", "SA", "SU"] as const;
export type Weekday = (typeof WEEKDAYS)[number];

/** The "which" of an "on the …" rule → `BYSETPOS`: first..fourth, or last (-1). */
export type SetPos = 1 | 2 | 3 | 4 | -1;

/** The day of an "on the …" rule: a single weekday, or one of the three groups the legacy dropdown adds. */
export type OnTheDay = Weekday | "DAY" | "WEEKDAY" | "WEEKENDDAY";

export type YearlyMode = "ON" | "ONTHE";
export type MonthlyMode = "ON" | "ONTHE";
export type EndMode = "NEVER" | "COUNT" | "UNTIL";

/** The top-level options the recurrence select offers: none, a plain frequency, or the customized panel. */
export type RecurrenceMode = "NONE" | RecurrenceFreq | "CUSTOMIZED";

/**
 * The rule as the customized editor holds it. `freq` null means "no recurrence". `interval` applies to
 * monthly/weekly/daily only (yearly repeats every year). The yearly/monthly `…Mode` picks which of the
 * two rows is active; `which`/`onTheDay` back the shared "on the …" row of both. `until`, when set, is an
 * inclusive last day as `yyyy-MM-dd`.
 */
export interface RecurrenceModel {
  freq: RecurrenceFreq | null;
  interval: number;
  weeklyDays: Weekday[];
  yearlyMode: YearlyMode;
  yearlyMonth: number;
  yearlyDay: number;
  monthlyMode: MonthlyMode;
  monthlyDay: number;
  which: SetPos;
  onTheDay: OnTheDay;
  endMode: EndMode;
  count: number;
  until: string | null;
}

/** A rule with no recurrence — the starting point and what an unparseable or unsupported rule reads as. */
export function emptyRecurrence(): RecurrenceModel {
  return {
    freq: null,
    interval: 1,
    weeklyDays: [],
    yearlyMode: "ON",
    yearlyMonth: 1,
    yearlyDay: 1,
    monthlyMode: "ON",
    monthlyDay: 1,
    which: 1,
    onTheDay: "MO",
    endMode: "NEVER",
    count: 1,
    until: null,
  };
}

const GROUP_INDICES: Record<"DAY" | "WEEKDAY" | "WEEKENDDAY", number[]> = {
  DAY: [0, 1, 2, 3, 4, 5, 6],
  WEEKDAY: [0, 1, 2, 3, 4],
  WEEKENDDAY: [5, 6],
};

/** The `BYDAY` weekday indices an "on the …" day expands to (a group to its several days). */
export function onTheDayToIndices(day: OnTheDay): number[] {
  if (day === "DAY" || day === "WEEKDAY" || day === "WEEKENDDAY")
    return [...GROUP_INDICES[day]];
  return [WEEKDAYS.indexOf(day)];
}

/** The reverse: the weekday indices of a parsed `BYDAY` back to a single weekday or the group it forms. */
export function indicesToOnTheDay(indices: number[]): OnTheDay {
  const key = [...indices].sort((a, b) => a - b).join(",");
  if (key === "0,1,2,3,4,5,6") return "DAY";
  if (key === "0,1,2,3,4") return "WEEKDAY";
  if (key === "5,6") return "WEEKENDDAY";
  const first = indices[0];
  return first != null && first >= 0 && first < WEEKDAYS.length
    ? WEEKDAYS[first]
    : "MO";
}
