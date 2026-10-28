import { describe, expect, it } from "vitest";
import { BAR_MAX_DAYS, dateRanges, daysBar } from "./absence-days-bar-model";

describe("daysBar", () => {
  it("marks weekends, uncovered days and the half day at the end", () => {
    const bar = daysBar({
      startDate: "2030-02-08",
      endDate: "2030-02-11",
      halfDayEnd: true,
      nonWorkingDays: ["2030-02-09", "2030-02-10"],
      uncoveredDays: ["2030-02-11"],
    });
    expect(bar).toEqual({
      truncated: false,
      days: [
        { date: "2030-02-08", kind: "work", half: false },
        { date: "2030-02-09", kind: "off", half: false },
        { date: "2030-02-10", kind: "off", half: false },
        { date: "2030-02-11", kind: "uncovered", half: true },
      ],
    });
  });

  it("draws no half day on a weekend", () => {
    const bar = daysBar({
      startDate: "2030-02-09",
      endDate: "2030-02-11",
      halfDayBegin: true,
      nonWorkingDays: ["2030-02-09", "2030-02-10"],
    });
    expect(bar?.days.map((d) => d.half)).toEqual([false, false, false]);
  });

  it("cuts a long vacation off at the same scale for every row", () => {
    const bar = daysBar({ startDate: "2030-01-01", endDate: "2030-12-31" });
    expect(bar?.days).toHaveLength(BAR_MAX_DAYS);
    expect(bar?.truncated).toBe(true);
  });

  it("draws nothing without a period", () => {
    expect(daysBar({ startDate: "2030-01-01" })).toBeNull();
  });
});

describe("dateRanges", () => {
  it("joins consecutive days, across a month's end", () => {
    expect(
      dateRanges(["2030-03-10", "2030-02-28", "2030-03-01", "2030-03-02"])
    ).toEqual([
      { from: "2030-02-28", to: "2030-03-02" },
      { from: "2030-03-10", to: "2030-03-10" },
    ]);
  });
});
