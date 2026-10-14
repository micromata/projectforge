"use client";

import type { CSSProperties, ReactNode } from "react";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatWeekdayShort, formatYearMonth } from "@/lib/format";
import { cn } from "@/lib/utils";
import { monthSpans } from "./vacation-chart-model";
import { dayClassName, GRID_CELL, gridCell } from "./vacation-chart-grid";
import type { VacationChartDay } from "./types";
import {
  DAY_COLUMN_MIN_WIDTH,
  type VacationChartDayWidth,
} from "./vacation-chart-width";

/** Rows 1–2 (months, days) of the names' column. */
const STICKY_CORNER = {
  "--chart-row": "1 / 3",
  "--chart-col": "1 / 2",
} as CSSProperties;

/**
 * The grid of a vacation chart with its axis — the months in row 1, the days in row 2 — and the rows passed as
 * [children] from grid row 3 on (see VacationChartRow). Scrolls sideways with the names' column staying at the
 * left. Shared by the chart of the vacation form (VacationOverlapChart) and the team calendar
 * (VacationTeamChart).
 *
 * @param dayWidth The width of the day columns (see VacationChartWidth): `narrow` without the weekday, `fit` in
 * the width of the window without the day numbers either (the holiday's tooltip stays).
 */
export function VacationChartFrame({
  days,
  ariaLabel,
  dayWidth = "normal",
  children,
}: {
  days: VacationChartDay[];
  ariaLabel: string;
  dayWidth?: VacationChartDayWidth;
  children: ReactNode;
}) {
  const ctx = useFormatContext();
  const columns = {
    "--chart-days": days.length,
    "--chart-day-width": DAY_COLUMN_MIN_WIDTH[dayWidth],
  } as CSSProperties;
  const fit = dayWidth === "fit";
  const weekday = dayWidth === "normal" || dayWidth === "wide";
  return (
    <div className="overflow-x-auto">
      <div
        role="img"
        aria-label={ariaLabel}
        // The names' column has a fixed width (`--chart-names`): the month labels stop right beside it.
        // `fit`: as wide as the window, not as its content (the month labels clip their names then).
        className={cn(
          "grid [--chart-names:7rem] [grid-template-columns:var(--chart-names)_repeat(var(--chart-days),minmax(var(--chart-day-width),1fr))] leading-tight sm:[--chart-names:10rem]",
          fit ? "w-full" : "min-w-max",
          weekday ? "text-xs" : "text-[10px]"
        )}
        style={columns}
      >
        {/* The corner above the names, covering the months and days that scroll beneath it. */}
        <div
          className={cn(GRID_CELL, "chart-panel-sticky sticky left-0 z-20")}
          style={STICKY_CORNER}
        />
        {monthSpans(days).map((span) => (
          <div
            key={span.month}
            // `overflow-clip`, not `hidden`: clipping without becoming a scroll container of its own, so
            // the label inside still sticks to the chart's scrolling.
            className={cn(
              GRID_CELL,
              "overflow-clip text-ellipsis whitespace-nowrap border-b border-l text-xs font-medium"
            )}
            style={gridCell(1, span.first + 2, span.last + 2)}
          >
            {/* Stays beside the names while its month is in view; the next month pushes it away. */}
            <span className="sticky left-[var(--chart-names)] px-1">
              {formatYearMonth(span.month, ctx)}
            </span>
          </div>
        ))}
        {days.map((day, i) => (
          <HintTooltip key={day.date} text={day.holiday} plain>
            <div
              className={cn(
                GRID_CELL,
                "flex flex-col items-center border-b tabular-nums",
                dayClassName(day),
                day.uncovered && "font-semibold text-destructive"
              )}
              style={gridCell(2, i + 2)}
            >
              {/* Keeps the row's height without the number. */}
              <span>{fit ? "\u00a0" : Number(day.date.slice(8, 10))}</span>
              {weekday && (
                <span className="text-[9px] text-muted-foreground">
                  {formatWeekdayShort(day.date, ctx).slice(0, 2)}
                </span>
              )}
            </div>
          </HintTooltip>
        ))}
        {children}
      </div>
    </div>
  );
}
