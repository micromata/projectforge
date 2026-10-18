"use client";

import type { CSSProperties, ReactNode } from "react";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatWeekdayShort, formatYearMonth } from "@/lib/format";
import { cn } from "@/lib/utils";
import { monthSpans } from "./absence-chart-model";
import { dayClassName, GRID_CELL, gridCell } from "./absence-chart-grid";
import type { ChartDay } from "./absence-chart-model";
import {
  DAY_COLUMN_MIN_WIDTH,
  type ChartDayWidth,
} from "./absence-chart-width";

/** The longest period printed with day numbers: a quarter, whose days are still 2–3mm wide on A4 landscape. */
const PRINT_NUMBERS_MAX_DAYS = 92;

/** Rows 1–2 (months, days) of the names' column. */
const STICKY_CORNER = {
  "--chart-row": "1 / 3",
  "--chart-col": "1 / 2",
} as CSSProperties;

/**
 * The grid of a vacation chart with its axis — the months in row 1, the days in row 2 — and the rows passed as
 * [children] from grid row 3 on (see AbsenceChartRow). Scrolls sideways with the names' column staying at the
 * left. Shared by the chart of the vacation form (VacationOverlapChart) and the team calendar
 * (VacationTeamChart).
 *
 * @param dayWidth The width of the day columns (see ChartWidth): `narrow` without the weekday, `fit` in
 * the width of the window without the day numbers either (the holiday's tooltip stays). Printed, the chart is
 * always laid out in the width of the page, whatever the width on screen: with small day numbers and without
 * weekdays up to a quarter, with the months only beyond (a year's days would be too narrow for a number).
 * @param role `img` (the form's chart, whose text alternative is the table of the substitutes' vacations below
 * it) or `group` for a chart that is its own text: the names and the labelled, focusable bars of the rows are
 * read, the axis is hidden from screen readers then (each bar names its dates).
 */
export function AbsenceChartFrame({
  days,
  ariaLabel,
  dayWidth = "normal",
  role = "img",
  children,
}: {
  days: ChartDay[];
  ariaLabel: string;
  dayWidth?: ChartDayWidth;
  role?: "img" | "group";
  children: ReactNode;
}) {
  const axisHidden = role === "group" ? true : undefined;
  const ctx = useFormatContext();
  const columns = {
    "--chart-days": days.length,
    "--chart-day-width": DAY_COLUMN_MIN_WIDTH[dayWidth],
  } as CSSProperties;
  const fit = dayWidth === "fit";
  const weekday = dayWidth === "normal" || dayWidth === "wide";
  const printNumbers = days.length <= PRINT_NUMBERS_MAX_DAYS;
  return (
    <div className="overflow-x-auto print:overflow-visible">
      <div
        role={role}
        aria-label={ariaLabel}
        // The names' column has a fixed width (`--chart-names`): the month labels stop right beside it.
        // `fit`: as wide as the window, not as its content (the month labels clip their names then).
        className={cn(
          "grid [--chart-names:7rem] [grid-template-columns:var(--chart-names)_repeat(var(--chart-days),minmax(var(--chart-day-width),1fr))] leading-tight sm:[--chart-names:10rem]",
          fit ? "w-full" : "min-w-max",
          weekday ? "text-xs" : "text-[10px]",
          // Not the variable: the screen's width is set inline, which no class overrides.
          "print:w-full print:min-w-0 print:text-[7px] print:[grid-template-columns:var(--chart-names)_repeat(var(--chart-days),minmax(0,1fr))]"
        )}
        style={columns}
      >
        {/* The corner above the names, covering the months and days that scroll beneath it. */}
        <div
          aria-hidden={axisHidden}
          className={cn(GRID_CELL, "chart-panel-sticky sticky left-0 z-20")}
          style={STICKY_CORNER}
        />
        {monthSpans(days).map((span) => (
          <div
            key={span.month}
            aria-hidden={axisHidden}
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
              aria-hidden={axisHidden}
            >
              {/* Hidden, not left out, without the number: it keeps the row's height. */}
              <span
                className={cn(
                  fit && "invisible",
                  printNumbers ? "print:visible" : "print:invisible"
                )}
              >
                {Number(day.date.slice(8, 10))}
              </span>
              {weekday && (
                <span className="text-[9px] text-muted-foreground print:hidden">
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
