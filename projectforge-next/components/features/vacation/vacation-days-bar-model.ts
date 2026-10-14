import { shiftDateByDays } from "@/lib/date-parse";

/** How a day of the list's bar is drawn. */
export type BarDayKind = "work" | "off" | "uncovered";

export interface BarDay {
  date: string;
  kind: BarDayKind;
  /** Half a day at the beginning or the end of the vacation: half the segment. */
  half: boolean;
}

export interface DaysBar {
  days: BarDay[];
  /** The vacation is longer than the bar shows. */
  truncated: boolean;
}

/** Days drawn at most: the same scale for every row, so the bars compare as durations. */
export const BAR_MAX_DAYS = 40;

/**
 * The days of a vacation as the list's bar draws them: a day without any substitute on duty red, a weekend or
 * holiday grey, any other a working day. ISO dates; null for an entry without a period.
 */
export function daysBar(vacation: {
  startDate?: string | null;
  endDate?: string | null;
  halfDayBegin?: boolean | null;
  halfDayEnd?: boolean | null;
  nonWorkingDays?: string[] | null;
  uncoveredDays?: string[] | null;
}): DaysBar | null {
  const { startDate, endDate } = vacation;
  if (!startDate || !endDate || endDate < startDate) return null;
  const off = new Set(vacation.nonWorkingDays ?? []);
  const uncovered = new Set(vacation.uncoveredDays ?? []);
  const days: BarDay[] = [];
  let date: string | null = startDate;
  while (date && date <= endDate && days.length < BAR_MAX_DAYS) {
    days.push({
      date,
      kind: uncovered.has(date) ? "uncovered" : off.has(date) ? "off" : "work",
      half:
        (date === startDate && !!vacation.halfDayBegin) ||
        (date === endDate && !!vacation.halfDayEnd),
    });
    date = shiftDateByDays(date, 1);
  }
  return { days, truncated: !!date && date <= endDate };
}

/** Consecutive ISO dates joined into periods, e.g. for "03.03.–05.03., 10.03.". */
export function dateRanges(dates: string[]): { from: string; to: string }[] {
  const ranges: { from: string; to: string }[] = [];
  for (const date of [...dates].sort()) {
    const last = ranges[ranges.length - 1];
    if (last && shiftDateByDays(last.to, 1) === date) last.to = date;
    else ranges.push({ from: date, to: date });
  }
  return ranges;
}
