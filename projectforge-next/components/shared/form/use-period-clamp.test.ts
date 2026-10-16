import { describe, expect, it } from "vitest";
import { clampPeriod } from "./use-period-clamp";

describe("clampPeriod", () => {
  it("sets an empty end to the start just set", () => {
    expect(clampPeriod({}, { start: "2026-10-20" })).toEqual({
      field: "end",
      value: "2026-10-20",
    });
  });

  it("moves the end onto a start set after it", () => {
    expect(
      clampPeriod(
        { start: "2026-10-20", end: "2026-10-22" },
        { start: "2026-10-25", end: "2026-10-22" }
      )
    ).toEqual({ field: "end", value: "2026-10-25" });
  });

  it("keeps an end after the start just set", () => {
    expect(
      clampPeriod(
        { start: "2026-10-20", end: "2026-10-30" },
        { start: "2026-10-25", end: "2026-10-30" }
      )
    ).toBeNull();
  });

  it("sets an empty start to the end just set, and moves it onto an end set before it", () => {
    expect(clampPeriod({}, { end: "2026-10-20" })).toEqual({
      field: "start",
      value: "2026-10-20",
    });
    expect(
      clampPeriod(
        { start: "2026-10-20", end: "2026-10-22" },
        { start: "2026-10-20", end: "2026-10-18" }
      )
    ).toEqual({ field: "start", value: "2026-10-18" });
  });

  it("touches nothing when nothing changed or a value was cleared", () => {
    const period = { start: "2026-10-20", end: "2026-10-22" };
    expect(clampPeriod(period, period)).toBeNull();
    expect(clampPeriod(period, { start: "2026-10-20", end: null })).toBeNull();
  });
});
