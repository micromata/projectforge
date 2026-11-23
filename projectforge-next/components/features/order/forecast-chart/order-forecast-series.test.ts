import { describe, expect, it } from "vitest";
import type { ForecastChartData } from "@/lib/rs/order";
import { hasForecastData, monthlyRows } from "./order-forecast-series";

const MONTHS = [
  "2025-11",
  "2025-12",
  "2026-01",
  "2026-02",
  "2026-03",
  "2026-04",
  "2026-05",
  "2026-06",
  "2026-07",
  "2026-08",
  "2026-09",
  "2026-10",
];

/** Chart data whose only remaining forecast is `value` in the month at `index`. */
function data(index: number, value: number): ForecastChartData {
  const values = MONTHS.map((_, i) => (i === index ? value : 0));
  return {
    months: MONTHS,
    forecastByStatus: { BEAUFTRAGT: values },
  } as unknown as ForecastChartData;
}

describe("hasForecastData", () => {
  it("ignores forecast values of past months", () => {
    expect(hasForecastData(data(2, 100), new Date(2026, 10, 23))).toBe(false);
  });

  it("counts forecast in the current or a later month", () => {
    expect(hasForecastData(data(11, 100), new Date(2026, 9, 5))).toBe(true);
    expect(hasForecastData(data(2, 100), new Date(2026, 0, 5))).toBe(true);
  });

  it("is false without any forecast", () => {
    expect(hasForecastData(data(11, 0), new Date(2025, 0, 1))).toBe(false);
  });
});

describe("monthlyRows", () => {
  it("sums the invoiced and all forecast parts of a month into stackSum", () => {
    const chart = {
      months: ["2026-11"],
      ist: [100],
      total: [0],
      prevYear: [0],
      prevPrevYear: [0],
      prevPrevPrevYear: [0],
      plan: null,
      forecastByStatus: { BEAUFTRAGT: [30], LOI: [5] },
    } as unknown as ForecastChartData;
    expect(monthlyRows(chart)[0].stackSum).toBe(135);
  });
});
