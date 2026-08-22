import { afterEach, describe, expect, it, vi } from "vitest";
import type { FormatContext } from "./format";
import { periodKindOf, type PeriodKind } from "./date-period";
import {
  anchorOfBounds,
  boundsOfPeriod,
  currentAnchorOf,
  endOfPeriod,
  periodOfBounds,
  shiftBounds,
} from "./date-period-bounds";
import { plusDays } from "./date-period-math";

const berlin: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };
const of = (id: string) => periodKindOf(id) as PeriodKind;
const month = of("month");
const yearToDate = of("yearToDate");
const week = of("termWeek");
const termMonth = of("termMonth");
const threeMonths = of("termThreeMonths");
const termYear = of("termYear");
const terms = [week, termMonth, threeMonths, termYear];

afterEach(() => {
  vi.useRealTimers();
});

describe("endOfPeriod", () => {
  it("ends the period that begins on the given day", () => {
    expect(endOfPeriod("2026-03-15", threeMonths, berlin)).toBe("2026-06-14");
    // Through the kind's own `beginOf`, so a snapping kind answers for its whole section.
    expect(endOfPeriod("2026-03-15", month, berlin)).toBe("2026-03-31");
  });

  it("has no end without a begin", () => {
    expect(endOfPeriod(null, termMonth, berlin)).toBeNull();
    expect(endOfPeriod(undefined, termMonth, berlin)).toBeNull();
    expect(endOfPeriod("", termMonth, berlin)).toBeNull();
  });

  it("has no end for text that is not a date yet", () => {
    // A date field holds text while it is being typed.
    expect(endOfPeriod("15.0", termMonth, berlin)).toBeNull();
    expect(endOfPeriod("tomorrow", termMonth, berlin)).toBeNull();
  });
});

describe("shiftBounds", () => {
  it("moves a term on by the term", () => {
    expect(
      shiftBounds("2026-03-15", "2026-06-14", threeMonths, 1, berlin)
    ).toEqual({ from: "2026-06-15", to: "2026-09-14" });
    expect(
      shiftBounds("2026-03-15", "2026-06-14", threeMonths, -1, berlin)
    ).toEqual({ from: "2025-12-15", to: "2026-03-14" });
  });

  it("stays where it is for no step at all", () => {
    expect(
      shiftBounds("2026-03-15", "2026-06-14", threeMonths, 0, berlin)
    ).toEqual({ from: "2026-03-15", to: "2026-06-14" });
  });

  it("moves a month on by a whole month, not by its days", () => {
    // The point of counting in months: March is 31 days long, and a shift by those would land on the
    // 1st of May.
    expect(
      shiftBounds("2026-03-01", "2026-03-31", termMonth, 1, berlin)
    ).toEqual({ from: "2026-04-01", to: "2026-04-30" });
    expect(
      shiftBounds("2026-03-15", "2026-04-14", termMonth, 1, berlin)
    ).toEqual({ from: "2026-04-15", to: "2026-05-14" });
    // And the calendar month is paged the same way, snapped to the first.
    expect(shiftBounds("2026-03-01", "2026-03-31", month, 1, berlin)).toEqual({
      from: "2026-04-01",
      to: "2026-04-30",
    });
  });

  it("moves a week on by seven days", () => {
    expect(shiftBounds("2026-03-02", "2026-03-08", week, 1, berlin)).toEqual({
      from: "2026-03-09",
      to: "2026-03-15",
    });
    expect(shiftBounds("2026-03-02", "2026-03-08", week, -2, berlin)).toEqual({
      from: "2026-02-16",
      to: "2026-02-22",
    });
  });

  it("counts a year on as twelve months", () => {
    expect(
      shiftBounds("2026-03-15", "2027-03-14", termYear, 1, berlin)
    ).toEqual({ from: "2027-03-15", to: "2028-03-14" });
  });

  it("moves a year to date a year back, ends and all", () => {
    // The case the kind exists for: the same window in the year before, both ends at once.
    vi.useFakeTimers().setSystemTime(new Date("2026-08-22T10:00:00.000Z"));
    expect(
      shiftBounds("2025-11-01", "2026-08-22", yearToDate, -1, berlin)
    ).toEqual({ from: "2024-11-01", to: "2025-08-22" });
  });

  it("clamps a term into a month that is shorter, and so does not lead back", () => {
    // `LocalDate.plusMonths` all over: the 31st of January one month on is the 28th of February, and
    // one back from there is the 28th of January rather than the 31st. Whoever wants that date types
    // it, which dissolves the term.
    const on = shiftBounds("2026-01-31", "2026-02-27", termMonth, 1, berlin);
    expect(on).toEqual({ from: "2026-02-28", to: "2026-03-27" });
    expect(shiftBounds(on?.from, on?.to, termMonth, -1, berlin)).toEqual({
      from: "2026-01-28",
      to: "2026-02-27",
    });
  });

  it("moves a range that is no period by the days it spans", () => {
    // 15.03. to 20.04. is 37 days, both ends counted — so the next range begins the day after it ends.
    expect(shiftBounds("2026-03-15", "2026-04-20", null, 1, berlin)).toEqual({
      from: "2026-04-21",
      to: "2026-05-27",
    });
    expect(shiftBounds("2026-03-15", "2026-04-20", null, -1, berlin)).toEqual({
      from: "2026-02-06",
      to: "2026-03-14",
    });
  });

  it("moves a single day on to the next one", () => {
    expect(shiftBounds("2026-03-15", "2026-03-15", null, 1, berlin)).toEqual({
      from: "2026-03-16",
      to: "2026-03-16",
    });
  });

  it("leaves a range of days exactly where it came from", () => {
    // Reversible, unlike the month cases: a length in days is the same length wherever it lands.
    let from = "2026-01-01";
    let to = "2026-02-11";
    for (let day = 0; day < 366; day++) {
      const on = shiftBounds(from, to, null, 1, berlin);
      expect(shiftBounds(on?.from, on?.to, null, -1, berlin), from).toEqual({
        from,
        to,
      });
      from = plusDays(from, 1);
      to = plusDays(to, 1);
    }
  });

  it("has nothing to move without both ends", () => {
    expect(shiftBounds("2026-03-15", null, termMonth, 1, berlin)).toBeNull();
    expect(shiftBounds(null, "2026-04-14", termMonth, 1, berlin)).toBeNull();
    expect(shiftBounds(null, null, null, 1, berlin)).toBeNull();
    expect(shiftBounds("2026-03-15", "", null, 1, berlin)).toBeNull();
  });

  it("has nothing to move on a bound that is not a date", () => {
    expect(shiftBounds("15.0", "2026-04-14", termMonth, 1, berlin)).toBeNull();
    expect(shiftBounds("2026-03-15", "tomorrow", null, 1, berlin)).toBeNull();
  });
});

describe("periodOfBounds", () => {
  it("recognises a whole calendar month", () => {
    expect(periodOfBounds("2026-08-01", "2026-08-31", [month], berlin)).toEqual(
      {
        kind: month,
        anchor: "2026-08-01",
      }
    );
  });

  it("recognises the term it wrote itself, for every begin of a year", () => {
    // The one case that catches the clamping being changed on one side only: every day of a year,
    // every term, there and back.
    for (const kind of terms) {
      let begin = "2026-01-01";
      for (let day = 0; day < 366; day++) {
        const end = kind.endOf(begin, berlin);
        expect(
          periodOfBounds(begin, end, terms, berlin)?.kind.id,
          `${begin} ${kind.id}`
        ).toBe(kind.id);
        begin = plusDays(begin, 1);
      }
    }
  });

  it("takes the first kind offered where two would fit", () => {
    // 01.03.–31.03. is a whole calendar month *and* a month from its begin — the price of one model
    // instead of two, and harmless: both page the aligned period the same way. Which name it gets is
    // the surface's choice, i.e. the order of the list it offers.
    expect(
      periodOfBounds("2026-03-01", "2026-03-31", [month, termMonth], berlin)
        ?.kind.id
    ).toBe("month");
    expect(
      periodOfBounds("2026-03-01", "2026-03-31", [termMonth, month], berlin)
        ?.kind.id
    ).toBe("termMonth");
  });

  it("never recognises the year to date, however the range ends", () => {
    // Its end moves with the calendar, so a range that happens to end today is no evidence — and it
    // would silently start paging by years. The kind comes only from what was stored.
    vi.useFakeTimers().setSystemTime(new Date("2026-08-22T10:00:00.000Z"));
    expect(
      periodOfBounds("2025-11-01", "2026-08-22", [yearToDate], berlin)
    ).toBeNull();
    expect(
      periodOfBounds("2026-01-01", "2026-08-22", [yearToDate, month], berlin)
    ).toBeNull();
  });

  it("does not recognise a range that only nearly is one", () => {
    expect(
      periodOfBounds("2026-08-01", "2026-08-30", [month], berlin)
    ).toBeNull();
    expect(
      periodOfBounds("2026-08-02", "2026-08-31", [month], berlin)
    ).toBeNull();
    expect(
      periodOfBounds("2026-03-15", "2026-04-20", terms, berlin)
    ).toBeNull();
    expect(
      periodOfBounds("2026-03-15", "2026-03-14", terms, berlin)
    ).toBeNull();
  });

  it("does not recognise a half-open range", () => {
    expect(periodOfBounds("2026-08-01", undefined, [month], berlin)).toBeNull();
    expect(periodOfBounds(undefined, "2026-08-31", [month], berlin)).toBeNull();
    expect(periodOfBounds(undefined, undefined, [month], berlin)).toBeNull();
  });

  it("recognises only what is offered", () => {
    expect(
      periodOfBounds("2026-03-15", "2026-06-14", [week, termMonth], berlin)
    ).toBeNull();
    expect(periodOfBounds("2026-08-01", "2026-08-31", [], berlin)).toBeNull();
  });

  it("survives a bound that is not a date", () => {
    expect(
      periodOfBounds("tomorrow", "2026-08-31", [month], berlin)
    ).toBeNull();
  });

  it("reads back every calendar period it writes", () => {
    for (let step = -13; step <= 13; step++) {
      const anchor = month.shift("2026-08-17", step, berlin);
      const bounds = boundsOfPeriod(month, anchor, berlin);
      expect(
        periodOfBounds(bounds.from, bounds.to, [month], berlin),
        anchor
      ).toEqual({ kind: month, anchor });
    }
  });
});

describe("anchorOfBounds", () => {
  it("takes the month of the start date", () => {
    expect(anchorOfBounds(month, "2026-03-17", undefined, berlin)).toBe(
      "2026-03-01"
    );
  });

  it("takes the start date itself for a kind that does not snap", () => {
    expect(anchorOfBounds(yearToDate, "2025-11-01", undefined, berlin)).toBe(
      "2025-11-01"
    );
  });

  it("prefers the start date over the end date", () => {
    // A range still being filled in: the month named must be the one the user is looking at, not the
    // one the other end happens to reach into.
    expect(anchorOfBounds(month, "2026-03-17", "2026-05-04", berlin)).toBe(
      "2026-03-01"
    );
  });

  it("falls back to the end date when only that is given", () => {
    expect(anchorOfBounds(month, undefined, "2026-05-04", berlin)).toBe(
      "2026-05-01"
    );
  });

  it("has nothing to say about an empty range", () => {
    expect(anchorOfBounds(month, undefined, null, berlin)).toBeNull();
  });

  it("skips a bound that is not a date", () => {
    // A date input holds text while it is being typed, so "17.0" reaches this before it is a date.
    expect(anchorOfBounds(month, "17.0", "2026-05-04", berlin)).toBe(
      "2026-05-01"
    );
    expect(anchorOfBounds(month, "tomorrow", undefined, berlin)).toBeNull();
  });

  it("is null where quick access is switched off", () => {
    expect(
      anchorOfBounds(undefined, "2026-03-17", undefined, berlin)
    ).toBeNull();
  });
});

describe("currentAnchorOf", () => {
  it("begins the month today lies in", () => {
    vi.useFakeTimers().setSystemTime(new Date("2026-08-17T10:00:00.000Z"));
    expect(currentAnchorOf(month, berlin)).toBe("2026-08-01");
  });

  it("asks a kind that does not snap what its current period is", () => {
    vi.useFakeTimers().setSystemTime(new Date("2026-08-17T10:00:00.000Z"));
    // The calendar year up to today: the neutral reading, and the one that needs no business year.
    expect(currentAnchorOf(yearToDate, berlin)).toBe("2026-01-01");
  });

  it("reads today from the user's zone, not the machine's", () => {
    // 19:00 UTC on 31 July is already 00:45 on 1 August in Kathmandu (+05:45), so the current month
    // is August there while it is still July in UTC.
    vi.useFakeTimers().setSystemTime(new Date("2026-07-31T19:00:00.000Z"));
    expect(
      currentAnchorOf(month, { locale: "de-DE", timeZone: "Asia/Kathmandu" })
    ).toBe("2026-08-01");
    expect(currentAnchorOf(month, { locale: "de-DE", timeZone: "UTC" })).toBe(
      "2026-07-01"
    );
  });

  it("begins a term today", () => {
    // Nothing to snap: a term picked with both boxes empty starts now (see `use-date-period-kind.ts`).
    vi.useFakeTimers().setSystemTime(new Date("2026-08-17T10:00:00.000Z"));
    expect(currentAnchorOf(termMonth, berlin)).toBe("2026-08-17");
  });
});
