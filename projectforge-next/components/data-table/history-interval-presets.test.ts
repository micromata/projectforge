import { describe, expect, it } from "vitest";
import type { FormatContext } from "@/lib/format";
import { INTERVAL_PRESETS } from "./history-interval-presets";

const berlin: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };
/** A fixed "now": 2026-07-15, 10:30 Berlin (+02:00). */
const NOW = "2026-07-15T08:30:00.000Z";

function fromOf(id: string, now = NOW, ctx = berlin): string | null {
  const preset = INTERVAL_PRESETS.find((p) => p.id === id);
  if (!preset) throw new Error(`no preset ${id}`);
  return preset.from(now, ctx);
}

describe("INTERVAL_PRESETS", () => {
  it("offers the periods the Wicket dropdown does", () => {
    expect(INTERVAL_PRESETS.map((p) => p.id)).toEqual([
      "lastMinute",
      "lastMinutes10",
      "lastMinutes30",
      "lastHour",
      "lastHours4",
      "today",
      "sinceYesterday",
      "lastDays3",
      "lastDays7",
      "lastDays14",
      "lastDays30",
      "lastDays60",
      "lastDays90",
    ]);
  });

  it("has unique ids", () => {
    const ids = INTERVAL_PRESETS.map((p) => p.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it("gives every plural label its placeholder argument, and no other one", () => {
    for (const preset of INTERVAL_PRESETS) {
      const plural = /s$/.test(preset.key);
      expect(
        preset.arg === undefined,
        `${preset.id}: arg vs ${preset.key}`
      ).toBe(!plural);
    }
  });

  it("counts minutes and hours back from now", () => {
    expect(fromOf("lastMinute")).toBe("2026-07-15T08:29:00.000Z");
    expect(fromOf("lastMinutes10")).toBe("2026-07-15T08:20:00.000Z");
    expect(fromOf("lastMinutes30")).toBe("2026-07-15T08:00:00.000Z");
    expect(fromOf("lastHour")).toBe("2026-07-15T07:30:00.000Z");
    expect(fromOf("lastHours4")).toBe("2026-07-15T04:30:00.000Z");
  });

  it("counts days back from now, keeping the time of day", () => {
    expect(fromOf("lastDays3")).toBe("2026-07-12T08:30:00.000Z");
    expect(fromOf("lastDays7")).toBe("2026-07-08T08:30:00.000Z");
    expect(fromOf("lastDays14")).toBe("2026-07-01T08:30:00.000Z");
    expect(fromOf("lastDays30")).toBe("2026-06-15T08:30:00.000Z");
    expect(fromOf("lastDays60")).toBe("2026-05-16T08:30:00.000Z");
    expect(fromOf("lastDays90")).toBe("2026-04-16T08:30:00.000Z");
  });

  it("starts 'today' at midnight in the user's zone", () => {
    // 00:00 Berlin on the 15th is 22:00 UTC on the 14th.
    expect(fromOf("today")).toBe("2026-07-14T22:00:00.000Z");
    expect(fromOf("sinceYesterday")).toBe("2026-07-13T22:00:00.000Z");
  });

  it("reads 'today' from the user's zone even when it differs from UTC's day", () => {
    // 23:30 UTC on the 15th is already the 16th in Berlin, so "today" starts on the 16th there.
    expect(fromOf("today", "2026-07-15T23:30:00.000Z")).toBe(
      "2026-07-15T22:00:00.000Z"
    );
    // …and in Kathmandu (+05:45) 19:00 UTC is likewise the next day.
    expect(
      fromOf("today", "2026-07-15T19:00:00.000Z", {
        locale: "de-DE",
        timeZone: "Asia/Kathmandu",
      })
    ).toBe("2026-07-15T18:15:00.000Z");
  });

  it("always produces a start before now", () => {
    for (const preset of INTERVAL_PRESETS) {
      const from = preset.from(NOW, berlin);
      expect(from, preset.id).not.toBeNull();
      expect(new Date(from!).getTime(), preset.id).toBeLessThan(
        new Date(NOW).getTime()
      );
    }
  });
});
