import { describe, expect, it } from "vitest";
import { niceDateTicks, niceScale } from "./chart-scale";

/** Every date from `start` (yyyy-MM-dd) for `count` consecutive days. */
function days(start: string, count: number): string[] {
  const base = new Date(`${start}T00:00:00Z`).getTime();
  return Array.from({ length: count }, (_, i) =>
    new Date(base + i * 86_400_000).toISOString().slice(0, 10)
  );
}

describe("niceScale", () => {
  it("rounds the step and bounds to 1/2/5 × 10ⁿ and includes 0", () => {
    // Range 0..1.6M over 6 ticks → raw step ~267k, rounded up to 500k.
    const { domain, ticks } = niceScale([0, 1_600_000, -300_000]);
    expect(domain).toEqual([-500_000, 2_000_000]);
    expect(ticks).toEqual([
      -500_000, 0, 500_000, 1_000_000, 1_500_000, 2_000_000,
    ]);
  });

  it("adds the zero line for an all-positive series", () => {
    const { domain, ticks } = niceScale([120_000, 260_000, 300_000]);
    expect(domain[0]).toBe(0);
    expect(ticks[0]).toBe(0);
  });

  it("gives an all-zero series a symmetric unit window", () => {
    expect(niceScale([0, 0])).toEqual({
      domain: [-1, 1],
      ticks: [-1, 0, 1],
    });
  });

  it("ignores non-finite values", () => {
    const { ticks } = niceScale([NaN, Infinity, 400_000]);
    expect(ticks.every((t) => Number.isFinite(t))).toBe(true);
    expect(ticks).toContain(0);
  });
});

describe("niceDateTicks", () => {
  it("snaps a half-year span to month firsts", () => {
    // Mid-June to late November — the arbitrary 11.07./07.08. ticks become clean month starts.
    const ticks = niceDateTicks(days("2026-06-15", 166));
    expect(ticks).toEqual([
      "2026-07-01",
      "2026-08-01",
      "2026-09-01",
      "2026-10-01",
      "2026-11-01",
    ]);
  });

  it("aligns a short span to Mondays", () => {
    // A 90-day span picks the 14-day step, aligned to Mondays.
    const ticks = niceDateTicks(days("2026-06-15", 90));
    expect(ticks.length).toBeGreaterThan(2);
    // 2026-06-15 is a Monday, so every tick is a Monday too.
    for (const iso of ticks) {
      expect(new Date(`${iso}T00:00:00Z`).getUTCDay()).toBe(1);
    }
  });

  it("only returns dates present in the input", () => {
    const input = days("2026-06-15", 166);
    const present = new Set(input);
    for (const iso of niceDateTicks(input)) {
      expect(present.has(iso)).toBe(true);
    }
  });

  it("labels every point when there are few", () => {
    const input = days("2026-06-15", 4);
    expect(niceDateTicks(input)).toEqual(input);
  });
});
