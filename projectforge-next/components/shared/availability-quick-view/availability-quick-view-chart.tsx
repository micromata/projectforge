"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateInput } from "@/lib/date-parse";
import { AbsenceChartFrame } from "@/components/shared/absence-chart/absence-chart-frame";
import { AbsenceChartLegend } from "@/components/shared/absence-chart/absence-chart-legend";
import { AbsenceChartRow } from "@/components/shared/absence-chart/absence-chart-row";
import {
  availabilityLabel,
  barLabel,
} from "@/components/shared/absence-chart/absence-chart-model";
import {
  teamChartRows,
  type TeamChartView,
} from "@/components/shared/absence-chart/absence-team-rows";
import type { ChartDayWidth } from "@/components/shared/absence-chart/absence-chart-width";

/** Day columns that fit the dialog: with weekdays for a month, the numbers for two, the days only for a quarter. */
function dayWidthOf(dayCount: number): ChartDayWidth {
  if (dayCount <= 31) return "normal";
  if (dayCount <= 62) return "narrow";
  return "fit";
}

/**
 * The bars of the user picked in the quick view (AvailabilityQuickViewDialog): their vacations, approved solid and in
 * progress pale, and their availability entries, as the team vacation calendar draws them. Without a single absence
 * a line says so: available all along.
 */
export function AvailabilityQuickViewChart({ view }: { view: TeamChartView }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const rows = useMemo(() => teamChartRows(view), [view]);
  const row = rows[0];
  if (!row) return null;
  const formatDate = (iso: string) => formatDateInput(iso, ctx);
  const statusLabel = (status: string) =>
    status === "APPROVED"
      ? t("vacation.status.approved")
      : status === "IN_PROGRESS"
        ? t("vacation.status.inProgress")
        : null;
  const empty = row.bars.length === 0 && !row.availabilities?.length;
  return (
    // `min-w-0`: a flex item would otherwise grow to the chart's width instead of letting it scroll.
    <figure className="chart-panel min-w-0 space-y-2 rounded-md border p-3">
      <AbsenceChartFrame
        days={view.days}
        ariaLabel={t("vacation.quickView.title")}
        dayWidth={dayWidthOf(view.days.length)}
        role="group"
      >
        <AbsenceChartRow
          row={row}
          gridRow={3}
          days={view.days}
          period={(bar) => barLabel(row.name, bar, formatDate, statusLabel)}
          availabilityText={(bar) =>
            availabilityLabel(row.name, bar, formatDate)
          }
          focusable
        />
      </AbsenceChartFrame>
      {empty ? (
        <p className="text-sm text-muted-foreground">
          {t("vacation.quickView.empty")}
        </p>
      ) : (
        <AbsenceChartLegend team availabilities />
      )}
    </figure>
  );
}
