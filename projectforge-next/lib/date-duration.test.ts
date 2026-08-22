import { describe, expect, it } from "vitest";
import { shiftDateByDays } from "./date-parse";
import {
  DURATION_IDS,
  DURATIONS,
  durationOf,
  durationOfBounds,
  durationsOf,
  endOfDuration,
  shiftBounds,
  type Duration,
  type DurationId,
} from "./date-duration";

const of = (id: DurationId) => durationOf(id) as Duration;
const week = of("week");
const month = of("month");
const threeMonths = of("threeMonths");
const year = of("year");

describe("DURATIONS", () => {
  it("offers a week, a month, three months and a year", () => {
    expect(DURATIONS.map((duration) => duration.id)).toEqual([
      "week",
      "month",
      "threeMonths",
      "year",
    ]);
    expect(DURATION_IDS).toEqual(DURATIONS.map((duration) => duration.id));
  });

  it("has unique ids", () => {
    const ids = DURATIONS.map((duration) => duration.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it("names every text as a key of its own, so the i18n scanner finds it", () => {
    for (const duration of DURATIONS) {
      for (const key of [duration.labelKey, duration.shortLabelKey]) {
        expect(key, duration.id).toMatch(/^[a-zA-Z0-9]+(\.[a-zA-Z0-9]+)+$/);
      }
    }
  });
});

describe("durationsOf", () => {
  it("resolves the ids named, in the order the durations are offered in", () => {
    expect(durationsOf(["year", "week"])).toEqual([week, year]);
  });

  it("offers nothing for an empty list or none at all", () => {
    expect(durationsOf([])).toEqual([]);
    expect(durationsOf(undefined)).toEqual([]);
  });

  it("drops an id that is not a duration", () => {
    expect(durationsOf(["quarter" as DurationId, "month"])).toEqual([month]);
  });
});

describe("durationOf", () => {
  it("finds the duration by its id", () => {
    expect(durationOf("threeMonths")).toEqual(threeMonths);
  });

  it("has nothing for no selection", () => {
    expect(durationOf(null)).toBeNull();
    expect(durationOf(undefined)).toBeNull();
    expect(durationOf("quarter" as DurationId)).toBeNull();
  });
});

describe("endOfDuration", () => {
  it("ends a week on the seventh day", () => {
    expect(endOfDuration("2026-03-15", week)).toBe("2026-03-21");
    expect(endOfDuration("2026-01-31", week)).toBe("2026-02-06");
  });

  it("ends a month on the day before the same day of the next one", () => {
    expect(endOfDuration("2026-03-15", month)).toBe("2026-04-14");
  });

  it("ends a month that begins on the first on the last of that month", () => {
    expect(endOfDuration("2026-03-01", month)).toBe("2026-03-31");
    expect(endOfDuration("2026-02-01", month)).toBe("2026-02-28");
    expect(endOfDuration("2024-02-01", month)).toBe("2024-02-29");
  });

  it("clamps a day the target month does not have", () => {
    // `LocalDate.of(2026, 1, 31).plusMonths(1).minusDays(1)`: the 28th of February, less a day.
    expect(endOfDuration("2026-01-31", month)).toBe("2026-02-27");
    expect(endOfDuration("2024-01-31", month)).toBe("2024-02-28");
    expect(endOfDuration("2026-03-31", month)).toBe("2026-04-29");
  });

  it("counts three months in whole months", () => {
    expect(endOfDuration("2026-03-15", threeMonths)).toBe("2026-06-14");
    expect(endOfDuration("2026-03-01", threeMonths)).toBe("2026-05-31");
    // Across the turn of the year, into a month that is three days shorter.
    expect(endOfDuration("2026-11-30", threeMonths)).toBe("2027-02-27");
  });

  it("counts a year as twelve months", () => {
    expect(endOfDuration("2026-03-15", year)).toBe("2027-03-14");
    expect(endOfDuration("2026-03-01", year)).toBe("2027-02-28");
    expect(endOfDuration("2024-02-29", year)).toBe("2025-02-27");
  });

  it("has no end without a begin", () => {
    expect(endOfDuration(null, month)).toBeNull();
    expect(endOfDuration(undefined, month)).toBeNull();
    expect(endOfDuration("", month)).toBeNull();
    // A date field holds text while it is being typed, so "15.0" can reach this.
    expect(endOfDuration("15.0", month)).toBeNull();
    expect(endOfDuration("tomorrow", month)).toBeNull();
  });
});

describe("shiftBounds", () => {
  it("moves a term on by the term", () => {
    expect(shiftBounds("2026-03-15", "2026-06-14", threeMonths, 1)).toEqual({
      from: "2026-06-15",
      to: "2026-09-14",
    });
    expect(shiftBounds("2026-03-15", "2026-06-14", threeMonths, -1)).toEqual({
      from: "2025-12-15",
      to: "2026-03-14",
    });
  });

  it("stays where it is for no step at all", () => {
    expect(shiftBounds("2026-03-15", "2026-06-14", threeMonths, 0)).toEqual({
      from: "2026-03-15",
      to: "2026-06-14",
    });
  });

  it("moves a month on by a whole month, not by its days", () => {
    // The point of counting in months: March is 31 days long, and a shift by those would land on the
    // 1st of May.
    expect(shiftBounds("2026-03-01", "2026-03-31", month, 1)).toEqual({
      from: "2026-04-01",
      to: "2026-04-30",
    });
    expect(shiftBounds("2026-03-15", "2026-04-14", month, 1)).toEqual({
      from: "2026-04-15",
      to: "2026-05-14",
    });
  });

  it("moves a week on by seven days", () => {
    expect(shiftBounds("2026-03-02", "2026-03-08", week, 1)).toEqual({
      from: "2026-03-09",
      to: "2026-03-15",
    });
    expect(shiftBounds("2026-03-02", "2026-03-08", week, -2)).toEqual({
      from: "2026-02-16",
      to: "2026-02-22",
    });
  });

  it("counts a year on as twelve months", () => {
    expect(shiftBounds("2026-03-15", "2027-03-14", year, 1)).toEqual({
      from: "2027-03-15",
      to: "2028-03-14",
    });
  });

  it("clamps a term into a month that is shorter, and so does not lead back", () => {
    // `LocalDate.plusMonths` all over: the 31st of January one month on is the 28th of February, and
    // one back from there is the 28th of January rather than the 31st. Whoever wants that date types
    // it, which dissolves the term.
    const on = shiftBounds("2026-01-31", "2026-02-27", month, 1);
    expect(on).toEqual({ from: "2026-02-28", to: "2026-03-27" });
    expect(shiftBounds(on?.from, on?.to, month, -1)).toEqual({
      from: "2026-01-28",
      to: "2026-02-27",
    });
  });

  it("moves a range that is no term by the days it spans", () => {
    // 15.03. to 20.04. is 37 days, both ends counted — so the next range begins the day after it ends.
    expect(shiftBounds("2026-03-15", "2026-04-20", null, 1)).toEqual({
      from: "2026-04-21",
      to: "2026-05-27",
    });
    expect(shiftBounds("2026-03-15", "2026-04-20", null, -1)).toEqual({
      from: "2026-02-06",
      to: "2026-03-14",
    });
  });

  it("moves a single day on to the next one", () => {
    expect(shiftBounds("2026-03-15", "2026-03-15", null, 1)).toEqual({
      from: "2026-03-16",
      to: "2026-03-16",
    });
  });

  it("leaves a range of days exactly where it came from", () => {
    // Reversible, unlike the month cases: a length in days is the same length wherever it lands.
    let from = "2026-01-01";
    let to = "2026-02-11";
    for (let day = 0; day < 366; day++) {
      const on = shiftBounds(from, to, null, 1);
      expect(shiftBounds(on?.from, on?.to, null, -1), `${from}`).toEqual({
        from,
        to,
      });
      from = shiftDateByDays(from, 1) as string;
      to = shiftDateByDays(to, 1) as string;
    }
  });

  it("has nothing to move without both ends", () => {
    expect(shiftBounds("2026-03-15", null, month, 1)).toBeNull();
    expect(shiftBounds(null, "2026-04-14", month, 1)).toBeNull();
    expect(shiftBounds(null, null, null, 1)).toBeNull();
    expect(shiftBounds("2026-03-15", "", null, 1)).toBeNull();
  });

  it("has nothing to move on a bound that is not a date", () => {
    expect(shiftBounds("15.0", "2026-04-14", month, 1)).toBeNull();
    expect(shiftBounds("2026-03-15", "tomorrow", null, 1)).toBeNull();
  });
});

describe("durationOfBounds", () => {
  it("recognises the term it wrote itself, for every begin of a year", () => {
    // The one test that catches the clamping being changed on one side only: every day of a year,
    // every duration, there and back.
    for (const duration of DURATIONS) {
      let begin = "2026-01-01";
      for (let day = 0; day < 366; day++) {
        const end = endOfDuration(begin, duration) as string;
        expect(
          durationOfBounds(begin, end, DURATIONS)?.id,
          `${begin} ${duration.id}`
        ).toBe(duration.id);
        begin = shiftDateByDays(begin, 1) as string;
      }
    }
  });

  it("recognises nothing in a range that is no term", () => {
    expect(durationOfBounds("2026-03-15", "2026-04-20", DURATIONS)).toBeNull();
    expect(durationOfBounds("2026-03-15", "2026-03-14", DURATIONS)).toBeNull();
  });

  it("recognises nothing in a half-open range", () => {
    expect(durationOfBounds("2026-03-15", null, DURATIONS)).toBeNull();
    expect(durationOfBounds(undefined, "2026-04-14", DURATIONS)).toBeNull();
    expect(durationOfBounds(null, null, DURATIONS)).toBeNull();
  });

  it("recognises only what is offered", () => {
    expect(
      durationOfBounds("2026-03-15", "2026-06-14", [week, month])
    ).toBeNull();
    expect(durationOfBounds("2026-03-15", "2026-06-14", [])).toBeNull();
  });

  it("survives a bound that is not a date", () => {
    expect(durationOfBounds("tomorrow", "2026-04-14", DURATIONS)).toBeNull();
  });
});
