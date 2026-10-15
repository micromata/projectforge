import { describe, expect, it } from "vitest";
import { isIsoDate } from "./date-period-math";

describe("isIsoDate", () => {
  it("takes the days that exist", () => {
    expect(isIsoDate("2026-01-01")).toBe(true);
    expect(isIsoDate("2026-12-31")).toBe(true);
    expect(isIsoDate("2028-02-29")).toBe(true);
  });

  it("rejects the shape without a day", () => {
    expect(isIsoDate("2026-13-01")).toBe(false);
    expect(isIsoDate("2026-00-10")).toBe(false);
    expect(isIsoDate("2026-02-29")).toBe(false);
    expect(isIsoDate("2026-04-31")).toBe(false);
    expect(isIsoDate("2026-04-00")).toBe(false);
  });

  it("rejects anything but exactly yyyy-MM-dd", () => {
    expect(isIsoDate("2026-4-01")).toBe(false);
    expect(isIsoDate("2026-04-01T00:00")).toBe(false);
    expect(isIsoDate("")).toBe(false);
    expect(isIsoDate(null)).toBe(false);
    expect(isIsoDate(undefined)).toBe(false);
  });
});
