"use client";

import { useMemo, type CSSProperties } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateInput } from "@/lib/date-parse";
import { formatWeekdayShort, formatYearMonth } from "@/lib/format";
import { cn } from "@/lib/utils";
import { buildVacationChart, monthSpans } from "./vacation-chart-model";
import { dayClassName, GRID_CELL, gridCell } from "./vacation-chart-grid";
import { VacationChartLegend } from "./vacation-chart-legend";
import { VacationChartRow } from "./vacation-chart-row";
import type { VacationValues } from "./vacation-schema";
import { useVacationStats } from "./use-vacation-stats";

/**
 * The vacation being edited and the vacations of its substitutes overlapping it, as bars over the
 * calendar days (`chartDays`, the period with three days on either side): which substitute is away when,
 * and on which days nobody is on duty. The vacation itself follows the form as it is typed; the
 * substitutes' come with the stats. The table below holds the same vacations with all their details.
 */
export function VacationOverlapChart() {
  const t = useTranslations();
  const ctx = useFormatContext();
  const form = useEntityEditForm();
  const stats = useVacationStats();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => state.values as VacationValues
  );
  const days = useMemo(() => stats?.chartDays ?? [], [stats?.chartDays]);
  const rows = useMemo(
    () =>
      buildVacationChart(
        days,
        values,
        [values.replacement, ...values.otherReplacements].filter(
          (e): e is NonNullable<typeof e> => e != null
        ),
        stats?.conflictingVacations ?? []
      ),
    [days, values, stats?.conflictingVacations]
  );
  if (days.length === 0 || !values.startDate || !values.endDate) return null;

  const columns = {
    "--chart-days": days.length,
  } as CSSProperties;
  return (
    <figure className="mb-4 space-y-2">
      <div className="overflow-x-auto">
        <div
          role="img"
          aria-label={t("vacation.chart.ariaLabel")}
          className="grid min-w-max [grid-template-columns:fit-content(10rem)_repeat(var(--chart-days),minmax(1.25rem,1fr))] text-xs leading-tight"
          style={columns}
        >
          {monthSpans(days).map((span) => (
            <div
              key={span.month}
              className={cn(
                GRID_CELL,
                "truncate border-b border-l px-1 font-medium"
              )}
              style={gridCell(1, span.first + 2, span.last + 2)}
            >
              {formatYearMonth(span.month, ctx)}
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
                <span>{Number(day.date.slice(8, 10))}</span>
                <span className="text-[9px] text-muted-foreground">
                  {formatWeekdayShort(day.date, ctx).slice(0, 2)}
                </span>
              </div>
            </HintTooltip>
          ))}
          {rows.map((row, r) => (
            <VacationChartRow
              key={row.key}
              row={row}
              gridRow={r + 3}
              days={days}
              period={(bar) =>
                `${formatDateInput(bar.startDate, ctx)} – ${formatDateInput(bar.endDate, ctx)}`
              }
            />
          ))}
        </div>
      </div>
      <VacationChartLegend />
    </figure>
  );
}
