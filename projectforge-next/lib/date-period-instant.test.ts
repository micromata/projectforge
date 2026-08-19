import { describe, expect, it } from "vitest";
import { PERIOD_UNITS, type PeriodUnit } from "./date-period";
import {
  anchorOfInstantBounds,
  instantBoundsOfPeriod,
  periodOfInstantBounds,
} from "./date-period-instant";
import type { FormatContext } from "./format";

const month = PERIOD_UNITS.find((unit) => unit.id === "month") as PeriodUnit;
const units = [month];

/** The zones lib/user-zone.test.ts uses: a DST one, an offset with minutes, and the south. */
const berlin: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };
const kathmandu: FormatContext = {
  locale: "de-DE",
  timeZone: "Asia/Kathmandu",
};
const auckland: FormatContext = {
  locale: "de-DE",
  timeZone: "Pacific/Auckland",
};

describe("instantBoundsOfPeriod", () => {
  it("spans a whole month from midnight to the last minute of its last day", () => {
    expect(instantBoundsOfPeriod(month, "2026-08-17", berlin)).toEqual({
      from: "2026-07-31T22:00:00.000Z",
      to: "2026-08-31T21:59:00.000Z",
    });
  });

  it("converts each end at its own offset when DST falls inside the month", () => {
    // Berlin goes forward on 29 March 2026: the first of the month is +01:00, the last +02:00. An
    // implementation that took one offset for both ends would be an hour out on one of them.
    expect(instantBoundsOfPeriod(month, "2026-03-15", berlin)).toEqual({
      from: "2026-02-28T23:00:00.000Z",
      to: "2026-03-31T21:59:00.000Z",
    });
    // …and the same the other way round in October, when the clocks go back.
    expect(instantBoundsOfPeriod(month, "2026-10-15", berlin)).toEqual({
      from: "2026-09-30T22:00:00.000Z",
      to: "2026-10-31T22:59:00.000Z",
    });
  });

  it("handles an offset that is not a whole hour", () => {
    expect(instantBoundsOfPeriod(month, "2026-08-17", kathmandu)).toEqual({
      from: "2026-07-31T18:15:00.000Z",
      to: "2026-08-31T18:14:00.000Z",
    });
  });

  it("handles a zone whose summer is the northern winter", () => {
    // Auckland goes back on 5 April 2026: +13:00 on the 1st, +12:00 on the 30th.
    expect(instantBoundsOfPeriod(month, "2026-04-15", auckland)).toEqual({
      from: "2026-03-31T11:00:00.000Z",
      to: "2026-04-30T11:59:00.000Z",
    });
  });

  it("falls back to the machine's zone when userData carries none", () => {
    const bounds = instantBoundsOfPeriod(month, "2026-08-17", {
      locale: "de-DE",
    });
    expect(bounds).not.toBeNull();
    expect(
      periodOfInstantBounds(bounds!.from, bounds!.to, units, {
        locale: "de-DE",
      })
    ).toEqual({ unit: month, anchor: "2026-08-01" });
  });
});

describe("periodOfInstantBounds", () => {
  it("recognises the month it wrote itself, in every zone", () => {
    for (const ctx of [berlin, kathmandu, auckland]) {
      for (const anchor of ["2026-01-01", "2026-03-01", "2026-08-01"]) {
        const bounds = instantBoundsOfPeriod(month, anchor, ctx);
        expect(bounds, `${ctx.timeZone} ${anchor}`).not.toBeNull();
        expect(
          periodOfInstantBounds(bounds!.from, bounds!.to, units, ctx),
          `${ctx.timeZone} ${anchor}`
        ).toEqual({ unit: month, anchor });
      }
    }
  });

  it("does not recognise an end that is not the last minute of its day", () => {
    // 22:59 UTC would be 00:59 Berlin — an hour short of the day's end.
    expect(
      periodOfInstantBounds(
        "2026-07-31T22:00:00.000Z",
        "2026-08-31T20:59:00.000Z",
        units,
        berlin
      )
    ).toBeNull();
    // Midnight of the following day: the same instant a Wicket "24:00" would mean, but not what
    // DateTimeInput writes, so no period is in effect.
    expect(
      periodOfInstantBounds(
        "2026-07-31T22:00:00.000Z",
        "2026-08-31T22:00:00.000Z",
        units,
        berlin
      )
    ).toBeNull();
  });

  it("does not recognise a begin that is not midnight", () => {
    expect(
      periodOfInstantBounds(
        "2026-08-01T06:00:00.000Z",
        "2026-08-31T21:59:00.000Z",
        units,
        berlin
      )
    ).toBeNull();
  });

  it("reads the bounds in the user's zone, not the machine's", () => {
    // A whole August in Berlin is not a whole month for a Kathmandu account: there the same two
    // instants are 01.08. 03:45 and 01.09. 03:44.
    const bounds = instantBoundsOfPeriod(month, "2026-08-01", berlin)!;
    expect(
      periodOfInstantBounds(bounds.from, bounds.to, units, berlin)
    ).not.toBeNull();
    expect(
      periodOfInstantBounds(bounds.from, bounds.to, units, kathmandu)
    ).toBeNull();
  });

  it("does not recognise a half-open or empty range", () => {
    const bounds = instantBoundsOfPeriod(month, "2026-08-01", berlin)!;
    expect(periodOfInstantBounds(bounds.from, null, units, berlin)).toBeNull();
    expect(
      periodOfInstantBounds(undefined, bounds.to, units, berlin)
    ).toBeNull();
    expect(periodOfInstantBounds(null, undefined, units, berlin)).toBeNull();
  });

  it("recognises nothing when no unit is offered", () => {
    const bounds = instantBoundsOfPeriod(month, "2026-08-01", berlin)!;
    expect(
      periodOfInstantBounds(bounds.from, bounds.to, [], berlin)
    ).toBeNull();
  });
});

describe("anchorOfInstantBounds", () => {
  it("takes the month of the lower bound", () => {
    expect(
      anchorOfInstantBounds(month, "2026-03-17T09:30:00.000Z", null, berlin)
    ).toBe("2026-03-01");
  });

  it("prefers the lower bound over the upper one", () => {
    expect(
      anchorOfInstantBounds(
        month,
        "2026-03-17T09:30:00.000Z",
        "2026-05-04T09:30:00.000Z",
        berlin
      )
    ).toBe("2026-03-01");
  });

  it("falls back to the upper bound alone", () => {
    expect(
      anchorOfInstantBounds(
        month,
        undefined,
        "2026-05-04T09:30:00.000Z",
        berlin
      )
    ).toBe("2026-05-01");
  });

  it("reads the day in the user's zone, not out of the string", () => {
    // 22:30 UTC on 31 July is already 00:30 on 1 August in Berlin (+02:00), so the input shows August
    // and the label has to agree. Taking the date out of the ISO string would name July.
    expect(
      anchorOfInstantBounds(month, "2026-07-31T22:30:00.000Z", null, berlin)
    ).toBe("2026-08-01");
    // And the other way round, west of UTC: 00:30 on 1 August is still 13:30 on 31 July in Midway
    // (-11:00), so there the label names July.
    expect(
      anchorOfInstantBounds(month, "2026-08-01T00:30:00.000Z", null, {
        locale: "de-DE",
        timeZone: "Pacific/Midway",
      })
    ).toBe("2026-07-01");
  });

  it("has nothing to say about an empty range", () => {
    expect(anchorOfInstantBounds(month, null, undefined, kathmandu)).toBeNull();
    expect(
      anchorOfInstantBounds(
        undefined,
        "2026-03-17T09:30:00.000Z",
        null,
        auckland
      )
    ).toBeNull();
  });
});
