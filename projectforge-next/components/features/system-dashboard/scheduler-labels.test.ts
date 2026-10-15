import { describe, expect, it } from "vitest";
import type { FormatContext } from "@/lib/format";
import { formatDurationMillis } from "./scheduler-labels";

const ctx = { locale: "en-US", timeZone: "UTC" } as FormatContext;

describe("formatDurationMillis", () => {
  it("is empty without a duration", () => {
    expect(formatDurationMillis(null, ctx)).toBe("");
    expect(formatDurationMillis(undefined, ctx)).toBe("");
  });

  it("chooses the unit by size", () => {
    expect(formatDurationMillis(850, ctx)).toBe("850 ms");
    expect(formatDurationMillis(12_340, ctx)).toBe("12.3 s");
    expect(formatDurationMillis(245_000, ctx)).toBe("4:05 min");
    expect(formatDurationMillis(3_720_000, ctx)).toBe("1:02 h");
  });
});
