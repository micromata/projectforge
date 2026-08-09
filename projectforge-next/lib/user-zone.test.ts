import { describe, expect, it } from "vitest";
import type { FormatContext } from "./format";
import {
  DEFAULT_TO_TIME,
  offsetMinutesAt,
  shiftIsoByMinutes,
  startOfDayIso,
  zonedIsoOf,
  zonedPartsOf,
} from "./user-zone";

/**
 * The user's zone, not the machine's — every case here uses a zone the CI runner is unlikely to be
 * in, which is exactly the bug class this module guards against.
 */
const berlin: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };
const kathmandu: FormatContext = {
  locale: "de-DE",
  // +05:45 — a half-hour zone catches offset arithmetic that only ever divided by whole hours.
  timeZone: "Asia/Kathmandu",
};
const auckland: FormatContext = {
  locale: "de-DE",
  // Southern hemisphere: DST runs the other way round, and the offset crosses the date line.
  timeZone: "Pacific/Auckland",
};

describe("offsetMinutesAt", () => {
  it("follows DST within one zone", () => {
    expect(offsetMinutesAt(new Date("2026-01-15T12:00:00Z"), berlin)).toBe(60);
    expect(offsetMinutesAt(new Date("2026-07-15T12:00:00Z"), berlin)).toBe(120);
  });

  it("handles a zone whose offset is not a whole hour", () => {
    expect(offsetMinutesAt(new Date("2026-07-15T12:00:00Z"), kathmandu)).toBe(
      345
    );
  });

  it("handles the southern hemisphere, where DST is inverted", () => {
    expect(offsetMinutesAt(new Date("2026-01-15T12:00:00Z"), auckland)).toBe(
      780
    );
    expect(offsetMinutesAt(new Date("2026-07-15T12:00:00Z"), auckland)).toBe(
      720
    );
  });

  it("falls back to the runtime zone when the user has none", () => {
    const date = new Date("2026-07-15T12:00:00Z");
    expect(offsetMinutesAt(date, { locale: "de-DE" })).toBe(
      -date.getTimezoneOffset()
    );
  });
});

describe("zonedIsoOf", () => {
  it("reads a wall clock as the user's zone, not as UTC", () => {
    // The bug this exists for: sent bare, "10:00" would be parsed as 10:00 UTC by the backend.
    expect(zonedIsoOf("2026-07-15", "10:00", berlin)).toBe(
      "2026-07-15T08:00:00.000Z"
    );
    expect(zonedIsoOf("2026-01-15", "10:00", berlin)).toBe(
      "2026-01-15T09:00:00.000Z"
    );
  });

  it("defaults a date entered without a time to midnight", () => {
    expect(zonedIsoOf("2026-07-15", undefined, berlin)).toBe(
      "2026-07-14T22:00:00.000Z"
    );
    expect(zonedIsoOf("2026-07-15", "", berlin)).toBe(
      "2026-07-14T22:00:00.000Z"
    );
  });

  it("takes the fallback time the caller passes, for the end of a range", () => {
    expect(zonedIsoOf("2026-07-15", undefined, berlin, DEFAULT_TO_TIME)).toBe(
      "2026-07-15T21:59:00.000Z"
    );
    // An entered time always wins over the fallback.
    expect(zonedIsoOf("2026-07-15", "08:15", berlin, DEFAULT_TO_TIME)).toBe(
      "2026-07-15T06:15:00.000Z"
    );
  });

  it("resolves a wall clock in the gap when the clocks go forward", () => {
    // 2026-03-29, Europe/Berlin jumps 02:00 → 03:00. 02:30 never happens; ZonedDateTime maps it
    // forward to 03:30 (+02:00), i.e. the same instant as 01:30 UTC.
    expect(zonedIsoOf("2026-03-29", "02:30", berlin)).toBe(
      "2026-03-29T01:30:00.000Z"
    );
  });

  it("picks the earlier of two identical wall clocks when the clocks go back", () => {
    // 2026-10-25, Berlin repeats 02:00–03:00. 02:30 exists twice: 00:30Z (+02:00) and 01:30Z
    // (+01:00). The first is the one ZonedDateTime resolves to.
    expect(zonedIsoOf("2026-10-25", "02:30", berlin)).toBe(
      "2026-10-25T00:30:00.000Z"
    );
  });

  it("handles a zone offset with minutes", () => {
    expect(zonedIsoOf("2026-07-15", "10:00", kathmandu)).toBe(
      "2026-07-15T04:15:00.000Z"
    );
  });

  it("rejects what is not a date", () => {
    expect(zonedIsoOf(null, "10:00", berlin)).toBeNull();
    expect(zonedIsoOf("", "10:00", berlin)).toBeNull();
    expect(zonedIsoOf("15.07.2026", "10:00", berlin)).toBeNull();
    expect(zonedIsoOf("2026-07-15", "nonsense", berlin)).toBeNull();
  });
});

describe("zonedPartsOf", () => {
  it("splits an instant into the wall clock the user reads", () => {
    expect(zonedPartsOf("2026-07-15T08:00:00.000Z", berlin)).toEqual({
      date: "2026-07-15",
      time: "10:00",
    });
    expect(zonedPartsOf("2026-01-15T09:00:00.000Z", berlin)).toEqual({
      date: "2026-01-15",
      time: "10:00",
    });
  });

  it("writes midnight as 00:00, not 24:00", () => {
    expect(zonedPartsOf("2026-07-14T22:00:00.000Z", berlin)).toEqual({
      date: "2026-07-15",
      time: "00:00",
    });
  });

  it("crosses the date boundary of a far-east zone", () => {
    expect(zonedPartsOf("2026-07-15T22:00:00.000Z", auckland)).toEqual({
      date: "2026-07-16",
      time: "10:00",
    });
  });

  it("round-trips with zonedIsoOf", () => {
    for (const ctx of [berlin, kathmandu, auckland]) {
      for (const iso of [
        "2026-01-15T09:00:00.000Z",
        "2026-07-15T08:00:00.000Z",
        "2026-10-25T00:30:00.000Z",
      ]) {
        const parts = zonedPartsOf(iso, ctx)!;
        expect(zonedIsoOf(parts.date, parts.time, ctx)).toBe(iso);
      }
    }
  });

  it("rejects what is not an instant", () => {
    expect(zonedPartsOf(null, berlin)).toBeNull();
    expect(zonedPartsOf("nonsense", berlin)).toBeNull();
  });
});

describe("startOfDayIso", () => {
  it("means midnight in the user's zone", () => {
    expect(startOfDayIso("2026-07-15T08:00:00.000Z", berlin)).toBe(
      "2026-07-14T22:00:00.000Z"
    );
  });

  it("steps back a day for 'since yesterday'", () => {
    expect(startOfDayIso("2026-07-15T08:00:00.000Z", berlin, -1)).toBe(
      "2026-07-13T22:00:00.000Z"
    );
  });

  it("steps across a month boundary", () => {
    expect(startOfDayIso("2026-08-01T08:00:00.000Z", berlin, -1)).toBe(
      "2026-07-30T22:00:00.000Z"
    );
  });

  it("uses the instant's own day in the user's zone, not the browser's", () => {
    // 23:30 UTC is already the next day in Berlin, so "today" is the 16th there.
    expect(startOfDayIso("2026-07-15T23:30:00.000Z", berlin)).toBe(
      "2026-07-15T22:00:00.000Z"
    );
  });
});

describe("shiftIsoByMinutes", () => {
  it("shifts an instant without touching the zone", () => {
    expect(shiftIsoByMinutes("2026-07-15T08:00:00.000Z", -30)).toBe(
      "2026-07-15T07:30:00.000Z"
    );
    expect(shiftIsoByMinutes("2026-07-15T08:00:00.000Z", 60 * 24)).toBe(
      "2026-07-16T08:00:00.000Z"
    );
  });

  it("stays an absolute shift across a DST change", () => {
    // 24 h before 2026-03-30T00:30Z is 2026-03-29T00:30Z, regardless of the clocks changing in
    // between — the periods are "last n hours", not "same time yesterday".
    expect(shiftIsoByMinutes("2026-03-30T00:30:00.000Z", -60 * 24)).toBe(
      "2026-03-29T00:30:00.000Z"
    );
  });
});
