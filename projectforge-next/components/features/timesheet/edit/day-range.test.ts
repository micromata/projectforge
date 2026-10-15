import { describe, expect, it } from "vitest";
import {
  DEFAULT_DURATION_MINUTES,
  durationMinutesOf,
  normalizedStopTime,
  stopTimeForNewStart,
} from "./day-range";

describe("durationMinutesOf", () => {
  it("returns the minutes between the two ends", () => {
    expect(
      durationMinutesOf("2026-08-09T08:00:00.000Z", "2026-08-09T09:30:00.000Z")
    ).toBe(90);
  });

  it("is negative when the ends are inverted", () => {
    expect(
      durationMinutesOf("2026-08-09T09:00:00.000Z", "2026-08-09T08:00:00.000Z")
    ).toBe(-60);
  });

  it("is null while either end is missing or unparseable", () => {
    expect(durationMinutesOf(null, "2026-08-09T09:00:00.000Z")).toBeNull();
    expect(durationMinutesOf("2026-08-09T08:00:00.000Z", null)).toBeNull();
    expect(
      durationMinutesOf("not-a-date", "2026-08-09T09:00:00.000Z")
    ).toBeNull();
  });
});

describe("stopTimeForNewStart", () => {
  it("keeps the length the sheet had when the start moves", () => {
    expect(
      stopTimeForNewStart("2026-08-09T10:00:00.000Z", {
        startTime: "2026-08-09T08:00:00.000Z",
        stopTime: "2026-08-09T09:30:00.000Z",
      })
    ).toBe("2026-08-09T11:30:00.000Z");
  });

  it("uses the default length when the previous span was not valid", () => {
    expect(
      stopTimeForNewStart("2026-08-09T10:00:00.000Z", {
        startTime: "2026-08-09T09:00:00.000Z",
        stopTime: "2026-08-09T08:00:00.000Z",
      })
    ).toBe(
      new Date(
        Date.parse("2026-08-09T10:00:00.000Z") +
          DEFAULT_DURATION_MINUTES * 60_000
      ).toISOString()
    );
  });

  it("keeps the previous stop when the start is cleared", () => {
    expect(
      stopTimeForNewStart(null, {
        startTime: "2026-08-09T08:00:00.000Z",
        stopTime: "2026-08-09T09:00:00.000Z",
      })
    ).toBe("2026-08-09T09:00:00.000Z");
  });
});

describe("normalizedStopTime", () => {
  it("leaves a stop after the start untouched", () => {
    expect(
      normalizedStopTime("2026-08-09T09:00:00.000Z", "2026-08-09T08:00:00.000Z")
    ).toBe("2026-08-09T09:00:00.000Z");
  });

  it("rolls a stop at or before the start to the next day", () => {
    expect(
      normalizedStopTime("2026-08-09T00:30:00.000Z", "2026-08-09T08:00:00.000Z")
    ).toBe("2026-08-10T00:30:00.000Z");
  });

  it("passes a missing end through unchanged", () => {
    expect(normalizedStopTime(null, "2026-08-09T08:00:00.000Z")).toBeNull();
    expect(normalizedStopTime("2026-08-09T09:00:00.000Z", null)).toBe(
      "2026-08-09T09:00:00.000Z"
    );
  });
});
