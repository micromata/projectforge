import { describe, expect, it } from "vitest";
import { chartWidthOf, resolveChartWidth } from "./absence-chart-width";

describe("resolveChartWidth", () => {
  it("draws a quarter normal and a year narrow by default", () => {
    expect(resolveChartWidth("auto", 92)).toBe("normal");
    expect(resolveChartWidth("auto", 365)).toBe("narrow");
  });

  it("keeps a width chosen", () => {
    expect(resolveChartWidth("wide", 365)).toBe("wide");
    expect(resolveChartWidth("fit", 90)).toBe("fit");
  });
});

describe("chartWidthOf", () => {
  it("reads a stored width, anything else as auto", () => {
    expect(chartWidthOf("narrow")).toBe("narrow");
    expect(chartWidthOf(null)).toBe("auto");
    expect(chartWidthOf("tiny")).toBe("auto");
  });
});
