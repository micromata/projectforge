"use client";

import type { CSSProperties } from "react";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { cn } from "@/lib/utils";
import type {
  AvailabilityBar,
  ChartBar,
  ChartRow,
} from "./absence-chart-model";
import type { ChartDay } from "./absence-chart-model";
import {
  ABSENT_BAR_CLASS,
  AVAILABILITY_BAR_CLASS,
  barClassName,
  dayClassName,
  GRID_CELL,
  gridCell,
} from "./absence-chart-grid";
import { inactiveRefClass } from "@/lib/page-def/reference";

/**
 * A row of the vacation chart: the name, the day columns' backgrounds and the bars of the vacations on top,
 * each spanning its days of the grid. A half day at a visible end leaves half of that day's column free.
 *
 * The availability entries are thin lines at the bottom of the row, beneath the vacations; a not-available one
 * (away as on vacation) is a striped bar of the vacations' height, beneath them, too: on days of both, the vacation
 * is shown (and its tooltip reached).
 *
 * @param focusable The bars take the keyboard focus, which opens their tooltip, and are named by it (`period`) —
 * for a chart that is its own text alternative (AbsenceChartFrame with role `group`).
 */
export function AbsenceChartRow({
  row,
  gridRow,
  days,
  period,
  availabilityText,
  focusable = false,
}: {
  row: ChartRow;
  gridRow: number;
  days: ChartDay[];
  period: (bar: ChartBar) => string;
  availabilityText?: (bar: AvailabilityBar) => string;
  focusable?: boolean;
}) {
  return (
    <>
      <HintTooltip text={row.name} plain>
        <div
          className={cn(
            GRID_CELL,
            // Stays at the left while the days scroll, above the bars (`z-10`) passing beneath it. The column
            // has a fixed width (see VacationOverlapChart); a longer name is cut, the tooltip has it whole.
            // Printed larger than the 7px of the printed axis (AbsenceChartFrame).
            "chart-panel-sticky sticky left-0 z-20 truncate border-b py-px pr-2 print:text-[10px]",
            row.own && "font-semibold"
          )}
          style={gridCell(gridRow, 1)}
        >
          <span className={inactiveRefClass(row.inactive)}>{row.name}</span>
        </div>
      </HintTooltip>
      {days.map((day, i) => (
        <div
          key={day.date}
          className={cn(GRID_CELL, "border-b", dayClassName(day))}
          style={gridCell(gridRow, i + 2)}
        />
      ))}
      {row.bars.map((bar) => {
        // Half of one day's column, as a share of the bar's own width.
        const half = `calc(100% / ${bar.last - bar.first + 1} / 2)`;
        const style = {
          ...gridCell(gridRow, bar.first + 2, bar.last + 2),
          "--bar-start": bar.halfFirst ? half : "0px",
          "--bar-end": bar.halfLast ? half : "0px",
        } as CSSProperties;
        const label = period(bar);
        return (
          <HintTooltip key={bar.key} text={label} plain>
            <div
              {...(focusable && {
                role: "img",
                tabIndex: 0,
                "aria-label": label,
              })}
              className={cn(
                GRID_CELL,
                "z-10 h-2.5 self-center rounded-sm focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring [margin-left:var(--bar-start)] [margin-right:var(--bar-end)]",
                barClassName(row.own, bar.status === "APPROVED", bar.conflict)
              )}
              style={style}
            />
          </HintTooltip>
        );
      })}
      {row.availabilities?.map((bar) => {
        const label = availabilityText?.(bar) ?? bar.label;
        return (
          <HintTooltip key={bar.key} text={label} plain>
            <div
              {...(focusable && {
                role: "img",
                tabIndex: 0,
                "aria-label": label,
              })}
              className={cn(
                GRID_CELL,
                "rounded-sm focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring",
                bar.absent
                  ? cn("z-[8] h-2.5 self-center", ABSENT_BAR_CLASS)
                  : cn("z-[5] mb-px h-1 self-end", AVAILABILITY_BAR_CLASS)
              )}
              style={gridCell(gridRow, bar.first + 2, bar.last + 2)}
            />
          </HintTooltip>
        );
      })}
    </>
  );
}
