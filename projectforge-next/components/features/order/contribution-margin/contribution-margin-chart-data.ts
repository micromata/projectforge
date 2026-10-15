import type { ChartConfig } from "@/components/ui/chart";
import { niceScale, type AxisScale } from "@/lib/chart-scale";
import { CHART_ROLE } from "@/lib/charts/roles";
import type { ContributionMarginData } from "@/lib/rs/order";

export const BAR_KEYS = ["revenue", "costs"] as const;

export const LINE_KEYS = ["profit", "prevYear"] as const;

/** The series keys of the DB % lines (period and previous year), plotted on the percent axis. */
export const PERCENTAGE_KEYS = ["percentage", "prevYearPercentage"] as const;

/** The axis id of the DB % line and the target bands; the amounts use recharts' default axis. */
export const PERCENT_AXIS = "percent";

/** The order of the legend entries, independent of the drawing order. */
export const LEGEND_ORDER = [
  "costs",
  "revenue",
  "percentage",
  "prevYearPercentage",
  "profit",
  "prevYear",
] as const;

/** The DB % lines are dotted, the DB1 lines (amounts) solid; the colour tells the year. */
export const PERCENT_DASH = "2 3";

/** The current year blue, the previous year green (darker than the revenue bars). */
const YEAR_COLOR = {
  current: "var(--chart-series-2)",
  prevYear: "var(--brand-green-dark)",
};

export interface ContributionMarginChartRow {
  month: string;
  revenue: number;
  costs: number;
  /** Null after the last month with values, so the line ends there instead of running on flat. */
  profit: number | null;
  prevYear: number;
  /** Null for a month without revenue, so the line has a gap there; 0 for a loss (sent so by the backend). */
  percentage: number | null;
  prevYearPercentage: number | null;
}

export function contributionMarginChartConfig(
  t: (key: string) => string
): ChartConfig {
  return {
    revenue: { label: t("revenue"), color: CHART_ROLE.positive },
    costs: { label: t("costs"), color: CHART_ROLE.negative },
    profit: { label: t("profit"), color: YEAR_COLOR.current },
    prevYear: { label: t("prevYear"), color: YEAR_COLOR.prevYear },
    percentage: { label: t("percentage"), color: YEAR_COLOR.current },
    prevYearPercentage: {
      label: t("prevYearPercentage"),
      color: YEAR_COLOR.prevYear,
    },
  };
}

export function contributionMarginChartRows(
  data: ContributionMarginData
): ContributionMarginChartRow[] {
  // The DB1 lines are cumulated from the first month on, like the DB % the backend sends; the bars stay monthly.
  let profit = 0;
  let prevYear = 0;
  // "yyyy-MM" compares like the months; no values at all if the period begins in the current month.
  const hasValues = (month: string) =>
    data.lastMonth === undefined ||
    (data.lastMonth != null && month <= data.lastMonth);
  return data.months.map((month, i) => ({
    month,
    revenue: data.revenue[i] ?? 0,
    costs: data.costs[i] ?? 0,
    profit: hasValues(month) ? (profit += data.profit[i] ?? 0) : null,
    prevYear: (prevYear += data.prevYear[i] ?? 0),
    percentage: data.percentage[i] ?? null,
    // Optional: missing in the response of a backend before the previous year's DB %.
    prevYearPercentage: data.prevYearPercentage?.[i] ?? null,
  }));
}

/** The scale of the amounts (bars and DB1 lines). */
export function amountScale(rows: ContributionMarginChartRow[]): AxisScale {
  return niceScale(
    rows.flatMap((row) => [
      ...BAR_KEYS.map((key) => row[key]),
      ...LINE_KEYS.flatMap((key) => row[key] ?? []),
    ])
  );
}

/**
 * The scale of the percent axis: from 0 up to 10 points above the target (or the highest monthly DB %,
 * previous year included), so the target line and both bands are always in view.
 */
export function percentScale(
  rows: ContributionMarginChartRow[],
  targetPercentage: number
): AxisScale {
  return niceScale(
    rows
      .flatMap((row) => PERCENTAGE_KEYS.map((key) => row[key]))
      .filter((value): value is number => value != null)
      .concat(0, targetPercentage + 10)
  );
}
