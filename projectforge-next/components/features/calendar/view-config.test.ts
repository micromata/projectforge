import { describe, expect, it } from "vitest";
import {
  clampVisibleEnd,
  compareAllDayByCategory,
  MAX_EVENT_RANGE_DAYS,
  normalizeInitialDate,
  scrollTime,
  slotDuration,
} from "./view-config";

describe("slotDuration", () => {
  it("pads minutes and rolls a full hour", () => {
    expect(slotDuration(5)).toBe("00:05:00");
    expect(slotDuration(15)).toBe("00:15:00");
    expect(slotDuration(30)).toBe("00:30:00");
    expect(slotDuration(60)).toBe("01:00:00");
  });

  it("falls back to 30 minutes for a nonsensical size", () => {
    expect(slotDuration(0)).toBe("00:30:00");
    expect(slotDuration(Number.NaN)).toBe("00:30:00");
  });
});

describe("scrollTime", () => {
  it("formats and clamps the first hour to 0..23", () => {
    expect(scrollTime(0)).toBe("00:00:00");
    expect(scrollTime(8)).toBe("08:00:00");
    expect(scrollTime(23)).toBe("23:00:00");
    expect(scrollTime(25)).toBe("23:00:00");
    expect(scrollTime(-1)).toBe("00:00:00");
  });
});

describe("normalizeInitialDate", () => {
  it("snaps a leading day of the previous month to the intended month in a month view", () => {
    // A January 2026 month grid opens on Mon Dec 29 2025; the intended month is January.
    const result = normalizeInitialDate("2025-12-29", "dayGridMonth");
    expect(result?.getFullYear()).toBe(2026);
    expect(result?.getMonth()).toBe(0);
    expect(result?.getDate()).toBe(1);
  });

  it("keeps a date that already is the first of a month", () => {
    const result = normalizeInitialDate("2026-02-01", "dayGridMonth");
    expect(result?.getMonth()).toBe(1);
    expect(result?.getDate()).toBe(1);
  });

  it("leaves non-month views untouched", () => {
    const result = normalizeInitialDate("2026-01-15", "timeGridWeek");
    expect(result?.getMonth()).toBe(0);
    expect(result?.getDate()).toBe(15);
  });

  it("returns undefined for a missing or unparseable date", () => {
    expect(normalizeInitialDate(undefined, "dayGridMonth")).toBeUndefined();
    expect(normalizeInitialDate("not-a-date", "dayGridMonth")).toBeUndefined();
  });
});

describe("clampVisibleEnd", () => {
  const start = new Date("2026-01-01T00:00:00Z");

  it("caps an over-long range at the 50-day boundary", () => {
    const end = new Date("2026-03-15T00:00:00Z"); // > 50 days
    const clamped = clampVisibleEnd(start, end);
    const days = (clamped.getTime() - start.getTime()) / (24 * 60 * 60 * 1000);
    expect(days).toBe(MAX_EVENT_RANGE_DAYS);
  });

  it("leaves a range within the boundary unchanged", () => {
    const end = new Date("2026-02-01T00:00:00Z"); // 31 days
    expect(clampVisibleEnd(start, end)).toBe(end);
  });
});

describe("compareAllDayByCategory", () => {
  const allDay = (category: string) => ({ allDay: 1, category });

  it("ranks calendar weeks, birthdays, holidays and vacations in that order", () => {
    const order = [
      "teamEvent",
      "vacation",
      "holiday",
      "address",
      "timesheet-stats",
    ]
      .map(allDay)
      .sort(compareAllDayByCategory)
      .map((e) => e.category);
    expect(order).toEqual([
      "timesheet-stats",
      "address",
      "holiday",
      "vacation",
      "teamEvent",
    ]);
  });

  it("treats unranked categories as equal so their default order survives", () => {
    expect(
      compareAllDayByCategory(allDay("teamEvent"), allDay("calEvent"))
    ).toBe(0);
  });

  it("leaves timed events untouched so the time grid stays chronological", () => {
    const timedFirst = { allDay: 0, category: "teamEvent" };
    expect(compareAllDayByCategory(timedFirst, allDay("timesheet-stats"))).toBe(
      0
    );
    expect(compareAllDayByCategory(allDay("timesheet-stats"), timedFirst)).toBe(
      0
    );
  });
});
