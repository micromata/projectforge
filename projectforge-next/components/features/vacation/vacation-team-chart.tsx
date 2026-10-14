"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateInput } from "@/lib/date-parse";
import { teamChartRows } from "./vacation-chart-model";
import { VacationChartFrame } from "./vacation-chart-frame";
import { VacationChartLegend } from "./vacation-chart-legend";
import { VacationChartRow } from "./vacation-chart-row";
import type { VacationExportView } from "./types";
import {
  resolveChartWidth,
  type VacationChartWidth,
} from "./vacation-chart-width";

/**
 * The team vacation calendar: one row per employee with their vacations as bars over the days of the period,
 * approved ones solid, those in progress pale. The axis and rows are the vacation form's chart
 * (VacationChartFrame, VacationChartRow), its day columns as wide as chosen (`auto`: narrow beyond a quarter).
 */
export function VacationTeamChart({
  view,
  dayWidth,
}: {
  view: VacationExportView;
  dayWidth: VacationChartWidth;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const rows = useMemo(() => teamChartRows(view), [view]);
  return (
    // `min-w-0`: a flex item would otherwise grow to the chart's width instead of letting it scroll.
    <figure className="chart-panel min-w-0 space-y-2 rounded-md border p-3">
      <VacationChartFrame
        days={view.days}
        ariaLabel={t("vacation.export.title")}
        dayWidth={resolveChartWidth(dayWidth, view.days.length)}
      >
        {rows.map((row, r) => (
          <VacationChartRow
            key={row.key}
            row={row}
            gridRow={r + 3}
            days={view.days}
            period={(bar) =>
              `${row.name}: ${formatDateInput(bar.startDate, ctx)} – ${formatDateInput(bar.endDate, ctx)}`
            }
          />
        ))}
      </VacationChartFrame>
      <VacationChartLegend team />
    </figure>
  );
}
