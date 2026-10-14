"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateInput } from "@/lib/date-parse";
import { barLabel, teamChartRows } from "./vacation-chart-model";
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
 * approved ones solid, those in progress pale and dashed. The axis and rows are the vacation form's chart
 * (VacationChartFrame, VacationChartRow), its day columns as wide as chosen (`auto`: narrow beyond a quarter).
 *
 * The chart is the page's content, so it is its own text alternative: a group of the employees' names and their
 * bars, each focusable and named with its period and status (barLabel), the axis hidden from screen readers.
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
  const statusLabel = (status: string) =>
    status === "APPROVED"
      ? t("vacation.status.approved")
      : status === "IN_PROGRESS"
        ? t("vacation.status.inProgress")
        : null;
  return (
    // `min-w-0`: a flex item would otherwise grow to the chart's width instead of letting it scroll.
    <figure className="chart-panel min-w-0 space-y-2 rounded-md border p-3">
      <VacationChartFrame
        days={view.days}
        ariaLabel={t("vacation.export.title")}
        dayWidth={resolveChartWidth(dayWidth, view.days.length)}
        role="group"
      >
        {rows.map((row, r) => (
          <VacationChartRow
            key={row.key}
            row={row}
            gridRow={r + 3}
            days={view.days}
            period={(bar) =>
              barLabel(
                row.name,
                bar,
                (iso) => formatDateInput(iso, ctx),
                statusLabel
              )
            }
            focusable
          />
        ))}
      </VacationChartFrame>
      <VacationChartLegend team />
    </figure>
  );
}
