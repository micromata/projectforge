"use client";

import type { CSSProperties } from "react";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { cn } from "@/lib/utils";
import type { ChartBar, ChartRow } from "./vacation-chart-model";
import type { VacationChartDay } from "./types";
import {
  barClassName,
  dayClassName,
  GRID_CELL,
  gridCell,
} from "./vacation-chart-grid";

/**
 * A row of the vacation chart: the name, the day columns' backgrounds and the bars of the vacations on top,
 * each spanning its days of the grid. A half day at a visible end leaves half of that day's column free.
 *
 * @param focusable The bars take the keyboard focus, which opens their tooltip, and are named by it (`period`) —
 * for a chart that is its own text alternative (VacationChartFrame with role `group`).
 */
export function VacationChartRow({
  row,
  gridRow,
  days,
  period,
  focusable = false,
}: {
  row: ChartRow;
  gridRow: number;
  days: VacationChartDay[];
  period: (bar: ChartBar) => string;
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
            "chart-panel-sticky sticky left-0 z-20 truncate border-b py-px pr-2",
            row.own && "font-semibold"
          )}
          style={gridCell(gridRow, 1)}
        >
          {row.name}
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
    </>
  );
}
