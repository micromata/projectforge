import type { FormatContext } from "@/lib/format";
import { shiftIsoByMinutes, startOfDayIso } from "@/lib/user-zone";

/**
 * The quick-select periods of the history filter, as Wicket's "Änderungszeitraum" dropdown and the
 * legacy DateTimeRange.jsx offer them. Every one ends at "now" and only differs in where it starts.
 *
 * The labels reuse the backend's `search.last*` keys, which carry a `{arg0}` placeholder for the
 * plural forms ("Letzte {arg0} Tage"). Nothing is hardcoded here beyond the numbers.
 */
export interface IntervalPreset {
  /** Stable id, only used as a React key. */
  id: string;
  /** i18n key under the `search` namespace. */
  key: string;
  /** Fills the `{arg0}` placeholder of the plural keys. */
  arg?: number;
  /** Start of the period, given "now" as an ISO instant. */
  from: (now: string, ctx: FormatContext) => string | null;
}

function minutesAgo(minutes: number) {
  return (now: string) => shiftIsoByMinutes(now, -minutes);
}

const HOUR = 60;
const DAY = 24 * HOUR;

export const INTERVAL_PRESETS: IntervalPreset[] = [
  { id: "lastMinute", key: "lastMinute", from: minutesAgo(1) },
  { id: "lastMinutes10", key: "lastMinutes", arg: 10, from: minutesAgo(10) },
  { id: "lastMinutes30", key: "lastMinutes", arg: 30, from: minutesAgo(30) },
  { id: "lastHour", key: "lastHour", from: minutesAgo(HOUR) },
  { id: "lastHours4", key: "lastHours", arg: 4, from: minutesAgo(4 * HOUR) },
  // Midnight in the *user's* zone, not the browser's — the two differ whenever the account is set
  // to another zone, and "today" then means the wrong day.
  { id: "today", key: "today", from: (now, ctx) => startOfDayIso(now, ctx) },
  {
    id: "sinceYesterday",
    key: "sinceYesterday",
    from: (now, ctx) => startOfDayIso(now, ctx, -1),
  },
  // "Last n days" counts from now, keeping the time of day, as the legacy component did — it is a
  // rolling window, not "since midnight n days ago".
  { id: "lastDays3", key: "lastDays", arg: 3, from: minutesAgo(3 * DAY) },
  { id: "lastDays7", key: "lastDays", arg: 7, from: minutesAgo(7 * DAY) },
  { id: "lastDays14", key: "lastDays", arg: 14, from: minutesAgo(14 * DAY) },
  { id: "lastDays30", key: "lastDays", arg: 30, from: minutesAgo(30 * DAY) },
  { id: "lastDays60", key: "lastDays", arg: 60, from: minutesAgo(60 * DAY) },
  { id: "lastDays90", key: "lastDays", arg: 90, from: minutesAgo(90 * DAY) },
];
