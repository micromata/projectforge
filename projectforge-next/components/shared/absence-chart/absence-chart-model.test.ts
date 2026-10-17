import { describe, expect, it } from "vitest";
import { buildAbsenceChart, type ChartDay } from "./absence-chart-model";

/** The days 2030-02-01 … 2030-02-<count>. */
function days(count: number): ChartDay[] {
  return Array.from({ length: count }, (_, i) => ({
    date: `2030-02-${String(i + 1).padStart(2, "0")}`,
    workingDay: true,
    uncovered: i === 2,
  }));
}

describe("buildAbsenceChart", () => {
  it("draws the edited entry first, then every substitute by its key, also one without an absence", () => {
    const rows = buildAbsenceChart(
      days(5),
      {
        name: "Own",
        startDate: "2030-02-02",
        endDate: "2030-02-04",
        status: "APPROVED",
      },
      [
        { key: "10", name: "Away" },
        { key: "11", name: "Freelancer" },
        { key: "12", name: "On duty" },
      ],
      [
        {
          id: 1,
          startDate: "2030-02-03",
          endDate: "2030-02-03",
          status: "APPROVED",
          person: { key: "10", name: "Away" },
        },
      ],
      [
        {
          startDate: "2030-02-01",
          endDate: "2030-02-09",
          label: "Not available",
          absent: true,
          person: { key: "11", name: "Freelancer" },
        },
      ]
    );
    expect(rows.map((r) => r.key)).toEqual(["own", "10", "11", "12"]);
    // The own bar in pieces by the uncovered day.
    expect(rows[0].bars.map((b) => [b.first, b.last, b.conflict])).toEqual([
      [1, 1, false],
      [2, 2, true],
      [3, 3, false],
    ]);
    expect(rows[1].bars.map((b) => b.key)).toEqual(["10-1"]);
    expect(
      rows[2].availabilities?.map((a) => [a.first, a.last, a.absent])
    ).toEqual([[0, 4, true]]);
    expect(rows[3].bars).toEqual([]);
  });

  it("shows an absence of somebody no longer chosen under its own person", () => {
    const rows = buildAbsenceChart(
      days(3),
      { name: "Own", startDate: "2030-02-01", endDate: "2030-02-03" },
      [],
      [
        {
          startDate: "2030-02-02",
          endDate: "2030-02-02",
          person: { key: "20", name: "Former" },
        },
      ]
    );
    expect(rows.map((r) => r.name)).toEqual(["Own", "Former"]);
  });
});
