import { afterEach, describe, expect, it, vi } from "vitest";
import type { FormatContext } from "./format";
import {
  anchorOfBounds,
  boundsOfPeriod,
  currentAnchorOf,
  PERIOD_UNITS,
  periodOfBounds,
  periodUnitsOf,
  type PeriodUnit,
} from "./date-period";

const berlin: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };
const month = PERIOD_UNITS.find((unit) => unit.id === "month") as PeriodUnit;

afterEach(() => {
  vi.useRealTimers();
});

describe("PERIOD_UNITS", () => {
  it("offers the month", () => {
    expect(PERIOD_UNITS.map((unit) => unit.id)).toEqual(["month"]);
  });

  it("has unique ids", () => {
    const ids = PERIOD_UNITS.map((unit) => unit.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it("names every text as a key of its own, so the i18n scanner finds it", () => {
    for (const unit of PERIOD_UNITS) {
      for (const key of [
        unit.labelKey,
        unit.tooltipPreviousKey,
        unit.tooltipCurrentKey,
        unit.tooltipNextKey,
      ]) {
        expect(key, unit.id).toMatch(/^[a-zA-Z0-9]+(\.[a-zA-Z0-9]+)+$/);
      }
    }
  });
});

describe("periodUnitsOf", () => {
  it("resolves the ids named", () => {
    expect(periodUnitsOf(["month"])).toEqual([month]);
  });

  it("offers nothing for an empty list or none at all", () => {
    expect(periodUnitsOf([])).toEqual([]);
    expect(periodUnitsOf(undefined)).toEqual([]);
  });

  it("drops a unit that does not exist yet", () => {
    expect(periodUnitsOf(["quarter", "month"])).toEqual([month]);
  });
});

describe("the month unit", () => {
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

  it("names the month in the user's locale", () => {
    expect(month.label("2026-08-01", berlin)).toBe("August 2026");
    expect(month.label("2026-12-01", { locale: "en-GB" })).toBe(
      "December 2026"
    );
  });
});

describe("periodOfBounds", () => {
  const units = [month];

  it("recognises a whole month", () => {
    expect(periodOfBounds("2026-08-01", "2026-08-31", units, berlin)).toEqual({
      unit: month,
      anchor: "2026-08-01",
    });
  });

  it("does not recognise a range that only nearly is one", () => {
    expect(
      periodOfBounds("2026-08-01", "2026-08-30", units, berlin)
    ).toBeNull();
    expect(
      periodOfBounds("2026-08-02", "2026-08-31", units, berlin)
    ).toBeNull();
    expect(
      periodOfBounds("2026-08-01", "2026-09-30", units, berlin)
    ).toBeNull();
  });

  it("does not recognise a half-open range", () => {
    expect(periodOfBounds("2026-08-01", undefined, units, berlin)).toBeNull();
    expect(periodOfBounds(undefined, "2026-08-31", units, berlin)).toBeNull();
    expect(periodOfBounds(undefined, undefined, units, berlin)).toBeNull();
  });

  it("recognises nothing when no unit is offered", () => {
    expect(periodOfBounds("2026-08-01", "2026-08-31", [], berlin)).toBeNull();
  });

  it("survives a bound that is not a date", () => {
    expect(periodOfBounds("tomorrow", "2026-08-31", units, berlin)).toBeNull();
  });

  it("reads back every period it writes", () => {
    for (let step = -13; step <= 13; step++) {
      const anchor = month.shift("2026-08-17", step, berlin);
      const bounds = boundsOfPeriod(month, anchor, berlin);
      expect(
        periodOfBounds(bounds.from, bounds.to, units, berlin),
        anchor
      ).toEqual({ unit: month, anchor });
    }
  });
});

describe("anchorOfBounds", () => {
  it("takes the month of the start date", () => {
    expect(anchorOfBounds(month, "2026-03-17", undefined, berlin)).toBe(
      "2026-03-01"
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

  it("reads today from the user's zone, not the machine's", () => {
    // 19:00 UTC on 31 July is already 00:45 on 1 August in Kathmandu (+05:45), so the current
    // month is August there while it is still July in UTC.
    vi.useFakeTimers().setSystemTime(new Date("2026-07-31T19:00:00.000Z"));
    expect(
      currentAnchorOf(month, { locale: "de-DE", timeZone: "Asia/Kathmandu" })
    ).toBe("2026-08-01");
    expect(currentAnchorOf(month, { locale: "de-DE", timeZone: "UTC" })).toBe(
      "2026-07-01"
    );
  });
});
