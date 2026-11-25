import { describe, expect, it } from "vitest";
import { periodKindOf, periodKindsOf } from "@/lib/date-period";
import type { FormatContext } from "@/lib/format";
import {
  clampToMin,
  customPeriodOf,
  initialExportPeriod,
  periodOfKind,
  stepCustomPeriod,
} from "./vacation-export-period";

const ctx: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };
const kinds = periodKindsOf(["quarter", "year"]);
const quarter = periodKindOf("quarter")!;
const params = (query: string) => new URLSearchParams(query);

describe("customPeriodOf", () => {
  it("keeps a range over the turn of the year", () => {
    expect(customPeriodOf("2026-11-01", "2027-01-31", null)).toEqual({
      period: { kind: null, from: "2026-11-01", until: "2027-01-31" },
      shortened: false,
    });
  });

  it("moves the begin to min and never ends before it begins", () => {
    expect(
      customPeriodOf("2025-12-01", "2025-12-05", "2026-01-01").period
    ).toEqual({
      kind: null,
      from: "2026-01-01",
      until: "2026-01-01",
    });
  });

  it("cuts the end to 400 days after the begin and says so", () => {
    const result = customPeriodOf("2026-01-01", "2028-01-01", null);
    expect(result.shortened).toBe(true);
    expect(result.period.until).toBe("2027-02-05");
  });
});

describe("initialExportPeriod", () => {
  it("reads a free range of the url", () => {
    const period = initialExportPeriod(
      params("period=custom&from=2026-11-01&until=2027-01-31"),
      null,
      kinds,
      null,
      ctx
    );
    expect(period).toEqual({
      kind: null,
      from: "2026-11-01",
      until: "2027-01-31",
    });
  });

  it("falls back to the quarter for an unreadable range", () => {
    for (const query of [
      "period=custom&from=2026-11-01",
      "period=custom&from=2026-13-01&until=2027-01-31",
      "period=custom&from=2027-01-31&until=2026-11-01",
    ]) {
      expect(
        initialExportPeriod(params(query), null, kinds, null, ctx).kind
      ).toBe(quarter);
    }
  });

  it("reads a quarter of the url", () => {
    expect(
      initialExportPeriod(
        params("period=quarter&from=2026-11-15"),
        null,
        kinds,
        null,
        ctx
      )
    ).toEqual({ kind: quarter, from: "2026-10-01", until: "2026-12-31" });
  });

  it("ignores a free range ending before min", () => {
    const period = initialExportPeriod(
      params("period=custom&from=2025-01-01&until=2025-02-01"),
      null,
      kinds,
      "2026-01-01",
      ctx
    );
    expect(period.kind).toBe(quarter);
  });
});

describe("clampToMin", () => {
  it("keeps a free range and replaces an old quarter", () => {
    const custom = { kind: null, from: "2025-01-01", until: "2025-02-01" };
    expect(clampToMin(custom, "2026-01-01", ctx)).toBe(custom);
    const old = periodOfKind(quarter, "2025-01-01", ctx);
    expect(clampToMin(old, "2026-01-01", ctx)).not.toEqual(old);
  });
});

describe("stepCustomPeriod", () => {
  const range = { kind: null, from: "2026-11-01", until: "2026-11-30" };

  it("pages by the days the range spans", () => {
    expect(stepCustomPeriod(range, 1, null, ctx)).toEqual({
      kind: null,
      from: "2026-12-01",
      until: "2026-12-30",
    });
  });

  it("stays where it is when the step would end before min", () => {
    expect(stepCustomPeriod(range, -1, "2026-11-01", ctx)).toBeNull();
  });
});
