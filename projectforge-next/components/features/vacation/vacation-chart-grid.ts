import type { CSSProperties } from "react";
import { cn } from "@/lib/utils";

// The cells and colours of the vacation chart (VacationOverlapChart), shared by its rows and its legend.

/** Background of a day's column: a day without any substitute on duty in red, weekend and holidays grey. */
export function dayClassName(day: {
  workingDay: boolean;
  uncovered?: boolean;
}) {
  if (day.uncovered) return "bg-destructive/15";
  return day.workingDay ? undefined : "bg-muted";
}

/** A cell of the chart's grid, placed through CSS variables (row, first and last column, both 1-based). */
export function gridCell(row: number, first: number, last = first) {
  return {
    "--chart-row": row,
    "--chart-col": `${first} / ${last + 1}`,
  } as CSSProperties;
}

/** The classes reading [gridCell]'s variables. */
export const GRID_CELL =
  "[grid-row:var(--chart-row)] [grid-column:var(--chart-col)]";

/**
 * An availability entry on the team calendar: a thin teal line at the bottom of the row, beneath the vacation bars,
 * so both stay visible on the same day (light blue in the Excel export).
 */
export const AVAILABILITY_BAR_CLASS = "bg-brand-teal";

/**
 * The vacation being edited in the primary colour — red on its days without any substitute on duty — the
 * substitutes' approved ones solid amber, the others pale. Brand colours, not the chart palette: those keep their
 * hue in dark mode, where the chart tokens turn grey.
 */
export function barClassName(
  own: boolean,
  approved: boolean,
  conflict = false
) {
  return cn(
    conflict ? "bg-destructive/70" : own ? "bg-primary" : "bg-brand-yellow",
    !approved && "border border-dashed border-foreground/40 opacity-50"
  );
}
