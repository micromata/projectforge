import { afterEach, describe, expect, it, vi } from "vitest";
import type { FormatContext } from "./format";
import { periodKindOf, type PeriodKind } from "./date-period";
import { boundsOfPeriod } from "./date-period-bounds";
import { plusDays } from "./date-period-math";

const berlin: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };
const of = (id: string) => periodKindOf(id) as PeriodKind;
const calWeek = of("week");
const month = of("month");
const yearToDate = of("yearToDate");
const week = of("termWeek");
const termMonth = of("termMonth");
const threeMonths = of("termThreeMonths");
const termYear = of("termYear");
const terms = [week, termMonth, threeMonths, termYear];

/** Freezes "today", which is what `yearToDate` reads — the rest of the kinds are pure calendar arithmetic. */
function today(iso: string): void {
  vi.useFakeTimers().setSystemTime(new Date(iso));
}

afterEach(() => {
  vi.useRealTimers();
});

describe("the calendar month", () => {
  it("spans the whole month a date lies in", () => {
    expect(boundsOfPeriod(month, "2026-08-17", berlin)).toEqual({
      from: "2026-08-01",
      to: "2026-08-31",
    });
  });

  it("knows the short months and the leap year", () => {
    expect(month.endOf("2026-02-10", berlin)).toBe("2026-02-28");
    expect(month.endOf("2024-02-10", berlin)).toBe("2024-02-29");
    expect(month.endOf("2026-04-30", berlin)).toBe("2026-04-30");
  });

  it("pages a month at a time", () => {
    expect(month.shift("2026-08-17", 1, berlin)).toBe("2026-09-01");
    expect(month.shift("2026-08-17", -1, berlin)).toBe("2026-07-01");
    expect(month.shift("2026-08-17", 0, berlin)).toBe("2026-08-01");
  });

  it("does not skip a month when the date is a long month's last day", () => {
    // What `date.setMonth(+1)` gets wrong: the 31st of January becomes the 3rd of March there.
    expect(month.shift("2026-01-31", 1, berlin)).toBe("2026-02-01");
    expect(month.shift("2026-05-31", 1, berlin)).toBe("2026-06-01");
    expect(month.shift("2026-03-31", -1, berlin)).toBe("2026-02-01");
  });

  it("pages across the turn of the year", () => {
    expect(month.shift("2026-12-15", 1, berlin)).toBe("2027-01-01");
    expect(month.shift("2026-01-15", -1, berlin)).toBe("2025-12-01");
    expect(month.shift("2026-06-15", -12, berlin)).toBe("2025-06-01");
  });
});

/** A calendar-aligned week, Monday to Sunday where the user starts on Monday — what a list filter asks. */
describe("the calendar week", () => {
  const sunday: FormatContext = { ...berlin, weekStartsOn: 0 };

  it("spans Monday to Sunday of the week a date lies in, Monday the default", () => {
    // 2026-08-17 is a Monday, 2026-08-19 the Wednesday of the same week.
    expect(boundsOfPeriod(calWeek, "2026-08-19", berlin)).toEqual({
      from: "2026-08-17",
      to: "2026-08-23",
    });
    expect(boundsOfPeriod(calWeek, "2026-08-17", berlin)).toEqual({
      from: "2026-08-17",
      to: "2026-08-23",
    });
  });

  it("snaps to the user's own first weekday", () => {
    // Same Wednesday, but a week that starts on Sunday runs 16.–22.
    expect(boundsOfPeriod(calWeek, "2026-08-19", sunday)).toEqual({
      from: "2026-08-16",
      to: "2026-08-22",
    });
  });

  it("pages a week at a time", () => {
    expect(calWeek.shift("2026-08-19", 1, berlin)).toBe("2026-08-24");
    expect(calWeek.shift("2026-08-19", -1, berlin)).toBe("2026-08-10");
    expect(calWeek.shift("2026-08-19", 0, berlin)).toBe("2026-08-17");
  });

  it("pages across the turn of the year", () => {
    // The week of 2026-01-01 (a Thursday) begins on Monday 2025-12-29.
    expect(boundsOfPeriod(calWeek, "2026-01-01", berlin)).toEqual({
      from: "2025-12-29",
      to: "2026-01-04",
    });
    expect(calWeek.shift("2026-01-01", -1, berlin)).toBe("2025-12-22");
  });

  it("has a current week, unlike a term", () => {
    expect(calWeek.tooltipCurrentKey).toBe(
      "calendar.quickselect.tooltip.selectCurrentWeek"
    );
    expect(calWeek.dependsOnToday).toBeUndefined();
  });
});

/**
 * The kind the whole comparison case is about: 01.11.2025 up to today, and one click back is the same
 * window a year earlier.
 */
describe("the year to date", () => {
  it("ends today when today is inside the year that began on the anchor", () => {
    today("2026-08-22T10:00:00.000Z");
    expect(boundsOfPeriod(yearToDate, "2025-11-01", berlin)).toEqual({
      from: "2025-11-01",
      to: "2026-08-22",
    });
  });

  it("ends in the anchor's own year when today falls after its day and month", () => {
    // A business year that began in March: today is later in the year than the anchor, so the window
    // is still the one that began this year.
    today("2026-08-22T10:00:00.000Z");
    expect(boundsOfPeriod(yearToDate, "2026-03-01", berlin)).toEqual({
      from: "2026-03-01",
      to: "2026-08-22",
    });
  });

  it("compares against the same window a year back", () => {
    today("2026-08-22T10:00:00.000Z");
    const previous = yearToDate.shift("2025-11-01", -1, berlin);
    expect(boundsOfPeriod(yearToDate, previous, berlin)).toEqual({
      from: "2024-11-01",
      to: "2025-08-22",
    });
  });

  it("leaves the anchor exactly where it was entered", () => {
    // No snapping anywhere: whatever begin stands in the box is where the year starts, which is why no
    // business year has to be configured.
    today("2026-08-22T10:00:00.000Z");
    expect(yearToDate.beginOf("2025-11-17", berlin)).toBe("2025-11-17");
  });

  it("pages by whole years, clamping the 29th of February", () => {
    expect(yearToDate.shift("2024-02-29", 1, berlin)).toBe("2025-02-28");
    expect(yearToDate.shift("2026-11-01", -3, berlin)).toBe("2023-11-01");
    expect(yearToDate.shift("2026-11-01", 0, berlin)).toBe("2026-11-01");
  });

  it("clamps an end the target year does not have", () => {
    // Today is the 29th of February and the window began in March, so the end is the 28th.
    today("2024-02-29T10:00:00.000Z");
    expect(yearToDate.endOf("2023-03-01", berlin)).toBe("2024-02-29");
    expect(yearToDate.endOf("2022-03-01", berlin)).toBe("2023-02-28");
  });

  it("ends on the anchor's own day where the window is a whole year old", () => {
    today("2026-11-01T10:00:00.000Z");
    expect(boundsOfPeriod(yearToDate, "2026-11-01", berlin)).toEqual({
      from: "2026-11-01",
      to: "2026-11-01",
    });
  });

  it("reads today from the user's zone, not the machine's", () => {
    // 19:00 UTC on 31 July is already 00:45 on 1 August in Kathmandu (+05:45).
    today("2026-07-31T19:00:00.000Z");
    expect(
      yearToDate.endOf("2026-01-01", {
        locale: "de-DE",
        timeZone: "Asia/Kathmandu",
      })
    ).toBe("2026-08-01");
    expect(
      yearToDate.endOf("2026-01-01", { locale: "de-DE", timeZone: "UTC" })
    ).toBe("2026-07-31");
  });

  it("starts in the calendar year with both boxes still empty", () => {
    today("2026-08-22T10:00:00.000Z");
    expect(yearToDate.currentAnchor?.(berlin)).toBe("2026-01-01");
  });

  it("is the one kind whose end moves with the calendar", () => {
    expect(yearToDate.dependsOnToday).toBe(true);
    for (const kind of [month, ...terms]) {
      expect(kind.dependsOnToday, kind.id).toBeUndefined();
    }
  });
});

/** A term: a length measured off the begin as it was given — what a Leistungszeitraum is. */
describe("the terms", () => {
  it("leaves the begin where it was entered", () => {
    for (const kind of terms) {
      expect(kind.beginOf("2026-03-15", berlin), kind.id).toBe("2026-03-15");
    }
  });

  it("ends a week on the seventh day", () => {
    expect(week.endOf("2026-03-15", berlin)).toBe("2026-03-21");
    expect(week.endOf("2026-01-31", berlin)).toBe("2026-02-06");
  });

  it("ends a month on the day before the same day of the next one", () => {
    expect(termMonth.endOf("2026-03-15", berlin)).toBe("2026-04-14");
  });

  it("ends a month that begins on the first on the last of that month", () => {
    expect(termMonth.endOf("2026-03-01", berlin)).toBe("2026-03-31");
    expect(termMonth.endOf("2026-02-01", berlin)).toBe("2026-02-28");
    expect(termMonth.endOf("2024-02-01", berlin)).toBe("2024-02-29");
  });

  it("clamps a day the target month does not have", () => {
    // `LocalDate.of(2026, 1, 31).plusMonths(1).minusDays(1)`: the 28th of February, less a day.
    expect(termMonth.endOf("2026-01-31", berlin)).toBe("2026-02-27");
    expect(termMonth.endOf("2024-01-31", berlin)).toBe("2024-02-28");
    expect(termMonth.endOf("2026-03-31", berlin)).toBe("2026-04-29");
  });

  it("counts three months in whole months", () => {
    expect(threeMonths.endOf("2026-03-15", berlin)).toBe("2026-06-14");
    expect(threeMonths.endOf("2026-03-01", berlin)).toBe("2026-05-31");
    // Across the turn of the year, into a month that is three days shorter.
    expect(threeMonths.endOf("2026-11-30", berlin)).toBe("2027-02-27");
  });

  it("counts a year as twelve months", () => {
    expect(termYear.endOf("2026-03-15", berlin)).toBe("2027-03-14");
    expect(termYear.endOf("2026-03-01", berlin)).toBe("2027-02-28");
    expect(termYear.endOf("2024-02-29", berlin)).toBe("2025-02-27");
  });

  it("moves the begin on by the term, so the next one starts the day after", () => {
    for (const kind of terms) {
      const on = kind.shift("2026-03-15", 1, berlin);
      expect(on, kind.id).toBe(plusDays(kind.endOf("2026-03-15", berlin), 1));
    }
  });

  it("says nothing about a current term, which there is none of", () => {
    for (const kind of terms) {
      expect(kind.tooltipCurrentKey, kind.id).toBeUndefined();
      expect(kind.currentAnchor, kind.id).toBeUndefined();
    }
  });

  it("counts the months it is named after", () => {
    expect(threeMonths.labelArg).toBe(3);
    expect(termMonth.labelArg).toBeUndefined();
  });

  it("throws on something that is not a date, for the caller that holds text", () => {
    // A date field holds text while it is being typed, so "15.0" reaches this; `endOfPeriod` is what
    // catches it (see ./date-period-bounds.test.ts).
    expect(() => termMonth.endOf("15.0", berlin)).toThrow();
    expect(() => termMonth.beginOf("tomorrow", berlin)).toThrow();
  });
});
