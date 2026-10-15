"use client";

import { useEffect, useMemo } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateInput } from "@/lib/date-parse";
import {
  availabilityLabel,
  barLabel,
  teamChartRows,
} from "./vacation-chart-model";
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
 * approved ones solid, those in progress pale and dashed, and — if asked for — their availability entries as thin
 * lines beneath. The axis and rows are the vacation form's chart
 * (VacationChartFrame, VacationChartRow), its day columns as wide as chosen (`auto`: narrow beyond a quarter).
 *
 * The chart is the page's content, so it is its own text alternative: a group of the employees' names and their
 * bars, each focusable and named with its period and status (barLabel), the axis hidden from screen readers.
 *
 * Printed — by the print button or the browser's own command — it is laid out in the width of the page (see
 * VacationChartFrame), and always light: a dark theme would print as a black page now that the backgrounds are
 * printed (see globals.css).
 */
export function VacationTeamChart({
  view,
  dayWidth,
  showAvailabilities = false,
}: {
  view: VacationExportView;
  dayWidth: VacationChartWidth;
  showAvailabilities?: boolean;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const rows = useMemo(() => teamChartRows(view), [view]);
  useEffect(() => {
    const root = document.documentElement;
    let dark = false;
    const beforePrint = () => {
      dark = root.classList.contains("dark");
      if (dark) root.classList.remove("dark");
    };
    const afterPrint = () => {
      if (dark) root.classList.add("dark");
      dark = false;
    };
    window.addEventListener("beforeprint", beforePrint);
    window.addEventListener("afterprint", afterPrint);
    return () => {
      window.removeEventListener("beforeprint", beforePrint);
      window.removeEventListener("afterprint", afterPrint);
    };
  }, []);
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
            availabilityText={(bar) =>
              availabilityLabel(row.name, bar, (iso) =>
                formatDateInput(iso, ctx)
              )
            }
            focusable
          />
        ))}
      </VacationChartFrame>
      <VacationChartLegend team availabilities={showAvailabilities} />
    </figure>
  );
}
