import type { ChartConfig } from "@/components/ui/chart";
import { formatMonthName, type FormatContext } from "@/lib/format";
import {
  FORECAST_CHART_STATUSES,
  type ForecastChartData,
  type ForecastChartStatus,
} from "@/lib/rs/order";

/**
 * The shaping of the forecast chart data (see `order-forecast-charts-view.tsx`) for its two charts, the
 * counterparts of sheet "Grafiken 1" of the forecast Excel export: the monthly stacked bars by position
 * status with the invoiced/plan/previous-year lines, and the cumulated curves.
 */

/** The i18n keys of the position statuses, spelled out so the i18n key scanner finds them. */
const STATUS_LABEL_KEYS: Record<ForecastChartStatus, string> = {
  BEAUFTRAGT: "fibu.auftrag.status.beauftragt",
  GELEGT: "fibu.auftrag.status.gelegt",
  LOI: "fibu.auftrag.status.loi",
  IN_ERSTELLUNG: "fibu.auftrag.status.in_erstellung",
  POTENZIAL: "fibu.auftrag.status.potenzial",
};

/** The colours of the Excel charts, one token per status and line (see `--chart-forecast-*` in `globals.css`). */
const STATUS_COLORS: Record<ForecastChartStatus, string> = {
  BEAUFTRAGT: "var(--chart-forecast-beauftragt)",
  GELEGT: "var(--chart-forecast-gelegt)",
  LOI: "var(--chart-forecast-loi)",
  IN_ERSTELLUNG: "var(--chart-forecast-in-erstellung)",
  POTENZIAL: "var(--chart-forecast-potenzial)",
};

const SERIES_COLORS = {
  ist: "var(--chart-forecast-ist)",
  total: "var(--chart-forecast-total)",
  plan: "var(--chart-forecast-plan)",
  prevYear: "var(--chart-forecast-prev-year)",
  prevPrevYear: "var(--chart-forecast-prev-prev-year)",
} as const;

export type SeriesKey = keyof typeof SERIES_COLORS;

/** The stacking order of the Excel chart's bars, bottom up; the invoiced sums (IST) sit on top. */
const BAR_STATUS_ORDER: ForecastChartStatus[] = [
  "BEAUFTRAGT",
  "LOI",
  "GELEGT",
  "POTENZIAL",
  "IN_ERSTELLUNG",
];

/**
 * The stacked bars of the monthly chart, bottom up: the statuses present in the data, then the invoiced sums
 * (IST) — as in the Excel, where past months show IST and future ones the remaining forecast.
 */
export function monthlyBarKeys(
  data: ForecastChartData
): (ForecastChartStatus | "ist")[] {
  return [
    ...BAR_STATUS_ORDER.filter((status) => data.forecastByStatus[status]),
    "ist",
  ];
}

/** The line keys of the monthly chart; the plan only if one was calculated. */
export function monthlyLineKeys(data: ForecastChartData): SeriesKey[] {
  return data.plan
    ? ["plan", "prevYear", "prevPrevYear"]
    : ["prevYear", "prevPrevYear"];
}

/** The line keys of the cumulated chart; the plan only if one was calculated. */
export function cumulativeLineKeys(data: ForecastChartData): SeriesKey[] {
  return data.plan
    ? ["total", "plan", "prevYear", "prevPrevYear"]
    : ["total", "prevYear", "prevPrevYear"];
}

/** The pre-previous year is drawn dotted, as in the Excel; all other lines are solid. */
export function lineDashArray(key: SeriesKey): string | undefined {
  return key === "prevPrevYear" ? "2 3" : undefined;
}

/** The chart config of both charts: labels and colours of all statuses and lines. */
export function buildForecastChartConfig(
  t: (key: string) => string
): ChartConfig {
  const config: ChartConfig = {};
  FORECAST_CHART_STATUSES.forEach((status) => {
    config[status] = {
      label: t(STATUS_LABEL_KEYS[status]),
      color: STATUS_COLORS[status],
    };
  });
  config.ist = {
    label: t("fibu.auftrag.forecast.chart.ist"),
    color: SERIES_COLORS.ist,
  };
  config.total = {
    label: t("fibu.auftrag.forecast.chart.total"),
    color: SERIES_COLORS.total,
  };
  config.plan = {
    label: t("fibu.auftrag.forecast.chart.plan"),
    color: SERIES_COLORS.plan,
  };
  config.prevYear = {
    label: t("fibu.auftrag.forecast.chart.prevYear"),
    color: SERIES_COLORS.prevYear,
  };
  config.prevPrevYear = {
    label: t("fibu.auftrag.forecast.chart.prevPrevYear"),
    color: SERIES_COLORS.prevPrevYear,
  };
  return config;
}

/** A row per month: the forecast of each status and the monthly values of the lines. */
export function monthlyRows(
  data: ForecastChartData
): Record<string, number | string>[] {
  return data.months.map((month, i) => {
    const row: Record<string, number | string> = { month };
    FORECAST_CHART_STATUSES.forEach((status) => {
      const values = data.forecastByStatus[status];
      if (values) {
        row[status] = values[i] ?? 0;
      }
    });
    row.ist = data.ist[i] ?? 0;
    row.prevYear = data.prevYear[i] ?? 0;
    row.prevPrevYear = data.prevPrevYear[i] ?? 0;
    if (data.plan) {
      row.plan = data.plan[i] ?? 0;
    }
    return row;
  });
}

/**
 * A row per month with the running totals ('Umsatz kumuliert' of the Excel): the forecast (per month the
 * max of invoiced and remaining forecast), the plan and the previous years.
 */
export function cumulativeRows(
  data: ForecastChartData
): Record<string, number | string>[] {
  const series: [SeriesKey, number[] | null][] = [
    ["total", data.total],
    ["plan", data.plan],
    ["prevYear", data.prevYear],
    ["prevPrevYear", data.prevPrevYear],
  ];
  const sums = series.map(() => 0);
  return data.months.map((month, i) => {
    const row: Record<string, number | string> = { month };
    series.forEach(([key, values], index) => {
      if (values) {
        sums[index] += values[i] ?? 0;
        row[key] = sums[index];
      }
    });
    return row;
  });
}

/**
 * A month of the x-axis ("yyyy-MM") as its name in the user's language, with the two-digit year if the
 * 12 months cross a year boundary.
 */
export function formatChartMonth(
  isoMonth: string,
  ctx: FormatContext,
  showYear: boolean
): string {
  const [year, month] = isoMonth.split("-");
  const name = formatMonthName(Number(month), ctx);
  return showYear ? `${name} '${year.slice(2)}` : name;
}

/** Whether the months span more than one calendar year. */
export function spansMultipleYears(data: ForecastChartData): boolean {
  return new Set(data.months.map((month) => month.slice(0, 4))).size > 1;
}
