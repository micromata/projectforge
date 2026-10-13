import { describe, expect, it } from "vitest";
import {
  buildVacationChart,
  monthSpans,
  type OwnVacation,
} from "./vacation-chart-model";
import type { VacationChartDay } from "./types";

/** The days 2030-02-01 … 2030-02-<count>. */
function days(count: number): VacationChartDay[] {
  return Array.from({ length: count }, (_, i) => ({
    date: `2030-02-${String(i + 1).padStart(2, "0")}`,
    workingDay: true,
  }));
}

const own: OwnVacation = {
  employee: { id: 1, displayName: "Own" },
  startDate: "2030-02-04",
  endDate: "2030-02-08",
  halfDayBegin: false,
  halfDayEnd: true,
  status: "IN_PROGRESS",
};

describe("buildVacationChart", () => {
  it("puts the own vacation first with its half day at the end", () => {
    const [row] = buildVacationChart(days(11), own, [], []);
    expect(row.own).toBe(true);
    expect(row.bars).toMatchObject([
      { first: 3, last: 7, halfFirst: false, halfLast: true },
    ]);
  });

  it("colours the uncovered days of the own vacation, keeping the half day at its end", () => {
    const axis = days(11).map((d, i) => ({
      ...d,
      uncovered: i === 5 || i === 6,
    }));
    const [row] = buildVacationChart(axis, own, [], []);
    expect(
      row.bars.map((b) => [b.first, b.last, b.conflict === true, b.halfLast])
    ).toEqual([
      [3, 4, false, false],
      [5, 6, true, false],
      [7, 7, false, true],
    ]);
  });

  it("lists every substitute, those on duty without a bar", () => {
    const rows = buildVacationChart(
      days(11),
      own,
      [
        { id: 2, displayName: "Two" },
        { id: 3, displayName: "Three" },
      ],
      [
        {
          id: 10,
          employee: { id: 2 },
          startDate: "2030-02-06",
          endDate: "2030-02-07",
          status: "APPROVED",
        },
      ]
    );
    expect(rows.map((r) => [r.name, r.bars.length])).toEqual([
      ["Own", 1],
      ["Two", 1],
      ["Three", 0],
    ]);
  });

  it("cuts a vacation off at the edges, without half days there", () => {
    const rows = buildVacationChart(
      days(11),
      own,
      [{ id: 2 }],
      [
        {
          id: 10,
          employee: { id: 2 },
          startDate: "2030-01-20",
          endDate: "2030-03-05",
          halfDayBegin: true,
          halfDayEnd: true,
        },
      ]
    );
    expect(rows[1].bars).toMatchObject([
      { first: 0, last: 10, halfFirst: false, halfLast: false },
    ]);
  });
});

describe("monthSpans", () => {
  it("groups the days by month", () => {
    expect(
      monthSpans([
        { date: "2030-01-30", workingDay: true },
        { date: "2030-01-31", workingDay: true },
        { date: "2030-02-01", workingDay: true },
      ])
    ).toEqual([
      { month: "2030-01", first: 0, last: 1 },
      { month: "2030-02", first: 2, last: 2 },
    ]);
  });
});
