import { describe, expect, it } from "vitest";
import {
  availabilityLabel,
  barLabel,
  monthSpans,
} from "@/components/shared/absence-chart/absence-chart-model";
import {
  buildVacationChart,
  teamChartRows,
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

  it("draws a single day with both half days as one half day, none on a weekend", () => {
    const single = {
      ...own,
      startDate: "2030-02-05",
      endDate: "2030-02-05",
      halfDayBegin: true,
    };
    expect(buildVacationChart(days(11), single, [], [])[0].bars).toMatchObject([
      { first: 4, last: 4, halfFirst: true, halfLast: false },
    ]);
    const axis = days(11).map((d, i) => ({ ...d, workingDay: i !== 7 }));
    expect(buildVacationChart(axis, own, [], [])[0].bars).toMatchObject([
      { first: 3, last: 7, halfFirst: false, halfLast: false },
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

  it("adds the substitutes' availability entries, a not-available one as absent, also of somebody not chosen", () => {
    const rows = buildVacationChart(
      days(11),
      own,
      [{ id: 2, displayName: "Two" }],
      [],
      [
        {
          employee: { id: 2, displayName: "Two" },
          startDate: "2030-02-05",
          endDate: "2030-02-06",
          label: "Not available",
          absent: true,
        },
        {
          employee: { id: 2, displayName: "Two" },
          startDate: "2030-02-09",
          endDate: "2030-03-01",
          label: "Remotely reachable",
        },
        {
          employee: { id: 4, displayName: "Four" },
          startDate: "2030-02-01",
          endDate: "2030-02-01",
          label: "Not available",
          absent: true,
        },
      ]
    );
    expect(rows.map((r) => r.name)).toEqual(["Own", "Two", "Four"]);
    expect(
      rows[1].availabilities?.map((a) => [a.first, a.last, a.absent, a.label])
    ).toEqual([
      [4, 5, true, "Not available"],
      [8, 10, false, "Remotely reachable"],
    ]);
    expect(rows[1].bars).toEqual([]);
    expect(rows[2].availabilities).toHaveLength(1);
    // Without any entry, the row has none.
    expect(rows[0].availabilities).toBeUndefined();
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

describe("teamChartRows", () => {
  it("draws every employee, also one without a vacation, cutting a vacation off at the period's edges", () => {
    const rows = teamChartRows({
      days: days(10),
      rows: [
        {
          employee: { id: 7, displayName: "Away" },
          vacations: [
            {
              id: 70,
              startDate: "2030-01-28",
              endDate: "2030-02-03",
              halfDayBegin: true,
              halfDayEnd: true,
              status: "APPROVED",
            },
            {
              id: 71,
              startDate: "2030-02-09",
              endDate: "2030-02-09",
              halfDayBegin: false,
              halfDayEnd: false,
              status: "IN_PROGRESS",
            },
          ],
        },
        { employee: { id: 8, displayName: "On duty" }, vacations: [] },
      ],
    });
    expect(rows.map((r) => [r.name, r.own, r.bars.length])).toEqual([
      ["Away", false, 2],
      ["On duty", false, 0],
    ]);
    // Begins before the period: no half day at its cut-off begin, but at its visible end.
    expect(rows[0].bars[0]).toMatchObject({
      first: 0,
      last: 2,
      halfFirst: false,
      halfLast: true,
      status: "APPROVED",
    });
    expect(rows[0].bars[1]).toMatchObject({
      first: 8,
      last: 8,
      status: "IN_PROGRESS",
    });
  });
});

describe("teamChartRows with availabilities", () => {
  it("draws the availability entries of a row, cut off at the period's edges, none if not sent", () => {
    const rows = teamChartRows({
      days: days(10),
      rows: [
        {
          employee: { id: 7, displayName: "Remote" },
          vacations: [],
          availabilities: [
            {
              id: 90,
              startDate: "2030-01-20",
              endDate: "2030-02-02",
              label: "Workation (Reachable if needed)",
              comment: "Lisbon",
              status: "REACHABLE_IF_NEEDED",
            },
            {
              id: 91,
              startDate: "2030-03-01",
              endDate: "2030-03-05",
              label: "Parental leave",
              status: "NOT_AVAILABLE",
            },
          ],
        },
        { employee: { id: 8, displayName: "On duty" }, vacations: [] },
      ],
    });
    expect(rows[0].availabilities).toEqual([
      {
        key: "7-a90",
        first: 0,
        last: 1,
        label: "Workation (Reachable if needed)",
        comment: "Lisbon",
        absent: false,
        startDate: "2030-01-20",
        endDate: "2030-02-02",
      },
    ]);
    expect(rows[1].availabilities).toEqual([]);
  });

  it("draws only the absences of the kinds asked for, also if the view still holds others", () => {
    const view = {
      days: days(10),
      rows: [
        {
          employee: { id: 7, displayName: "Remote" },
          vacations: [
            {
              id: 70,
              startDate: "2030-02-05",
              endDate: "2030-02-05",
              halfDayBegin: false,
              halfDayEnd: false,
              status: "APPROVED" as const,
            },
          ],
          availabilities: [
            {
              id: 90,
              startDate: "2030-02-01",
              endDate: "2030-02-02",
              label: "Workation",
              status: "REMOTE_REACHABLE",
            },
            {
              id: 91,
              startDate: "2030-02-07",
              endDate: "2030-02-08",
              label: "Not available",
              absent: true,
              status: "NOT_AVAILABLE",
            },
            // Without a known status: never drawn by kind.
            {
              id: 92,
              startDate: "2030-02-09",
              endDate: "2030-02-09",
              label: "?",
            },
          ],
        },
      ],
    };
    const keys = (kinds: Parameters<typeof teamChartRows>[1]) => {
      const [row] = teamChartRows(view, kinds);
      return [...row.bars, ...(row.availabilities ?? [])].map((bar) => bar.key);
    };
    expect(keys(["VACATION"])).toEqual(["7-70"]);
    expect(keys(["NOT_AVAILABLE"])).toEqual(["7-a91"]);
    expect(keys(["VACATION", "REMOTE_REACHABLE"])).toEqual(["7-70", "7-a90"]);
    expect(keys(undefined)).toEqual(["7-70", "7-a90", "7-a91"]);
  });
});

describe("availabilityLabel", () => {
  const formatDate = (iso: string) => iso.split("-").reverse().join(".");
  const bar = {
    label: "Parental leave",
    startDate: "2030-02-04",
    endDate: "2030-02-08",
  };

  it("names whose entry it is, what, when and the comment if any", () => {
    expect(
      availabilityLabel("Ann", { ...bar, comment: "Back in March" }, formatDate)
    ).toBe("Ann: Parental leave, 04.02.2030 – 08.02.2030 (Back in March)");
    expect(
      availabilityLabel("Ann", { ...bar, comment: null }, formatDate)
    ).toBe("Ann: Parental leave, 04.02.2030 – 08.02.2030");
  });
});

describe("barLabel", () => {
  const formatDate = (iso: string) => iso.split("-").reverse().join(".");
  const statusLabel = (status: string) =>
    status === "APPROVED"
      ? "approved"
      : status === "IN_PROGRESS"
        ? "in progress"
        : null;
  const bar = { startDate: "2030-02-04", endDate: "2030-02-08" };

  it("names whose vacation it is, its period and its status", () => {
    expect(
      barLabel(
        "Ann",
        { ...bar, status: "IN_PROGRESS" },
        formatDate,
        statusLabel
      )
    ).toBe("Ann: 04.02.2030 – 08.02.2030 (in progress)");
  });

  it("leaves out a status without a label, and a missing one", () => {
    expect(
      barLabel("Ann", { ...bar, status: "REJECTED" }, formatDate, statusLabel)
    ).toBe("Ann: 04.02.2030 – 08.02.2030");
    expect(
      barLabel("Ann", { ...bar, status: null }, formatDate, statusLabel)
    ).toBe("Ann: 04.02.2030 – 08.02.2030");
  });
});
