/**
 * The widths of the day columns the team vacation calendar offers (VacationChartWidthSelect), stored with the
 * choice of employees and groups (`VacationExportRequest.dayWidth`).
 *
 * - `auto`: `normal` for a quarter, `narrow` for anything longer (a year).
 * - `fit`: the whole period in the width of the window, without the day numbers.
 * - `narrow`: day numbers without the weekday; `normal` and `wide` with both.
 */
export const CHART_WIDTHS = [
  "auto",
  "fit",
  "narrow",
  "normal",
  "wide",
] as const;

export type ChartWidth = (typeof CHART_WIDTHS)[number];

/** A width as drawn: `auto` resolved. */
export type ChartDayWidth = Exclude<ChartWidth, "auto">;

/** The longest period `auto` draws `normal`: a quarter. */
const AUTO_NORMAL_MAX_DAYS = 92;

/** The minimum width of a day column; `fit` has none, the columns share the window's width. */
export const DAY_COLUMN_MIN_WIDTH: Record<ChartDayWidth, string> = {
  fit: "0px",
  narrow: "0.875rem",
  normal: "1.25rem",
  wide: "1.75rem",
};

/** The width stored, or `auto` for none or one no longer offered. */
export function chartWidthOf(value: string | null | undefined): ChartWidth {
  return CHART_WIDTHS.find((w) => w === value) ?? "auto";
}

export function resolveChartWidth(
  width: ChartWidth,
  dayCount: number
): ChartDayWidth {
  if (width !== "auto") return width;
  return dayCount > AUTO_NORMAL_MAX_DAYS ? "narrow" : "normal";
}
