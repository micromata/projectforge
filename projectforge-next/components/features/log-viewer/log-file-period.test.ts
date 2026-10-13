import { describe, expect, it } from "vitest";
import type { FormatContext } from "@/lib/format";
import { CUSTOM_PERIOD, logFileBounds } from "./log-file-period";

const ctx: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };
const now = "2026-10-12T10:30:00.000Z";

describe("logFileBounds", () => {
  it("starts a preset from now, open to now", () => {
    expect(logFileBounds("lastHour", null, null, ctx, now)).toEqual({
      from: Date.parse("2026-10-12T09:30:00.000Z"),
      until: null,
    });
  });

  it("reads today in the user's zone and falls back to it", () => {
    const today = { from: Date.parse("2026-10-11T22:00:00.000Z"), until: null };
    expect(logFileBounds("today", 1, 2, ctx, now)).toEqual(today);
    expect(logFileBounds(null, null, null, ctx, now)).toEqual(today);
    expect(logFileBounds("lastDays90", null, null, ctx, now)).toEqual(today);
  });

  it("takes the entered bounds of a custom period", () => {
    expect(logFileBounds(CUSTOM_PERIOD, 1, null, ctx, now)).toEqual({
      from: 1,
      until: null,
    });
  });
});
