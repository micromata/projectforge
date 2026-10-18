"use client";

import { useEffect, useMemo } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateInput } from "@/lib/date-parse";
import {
  availabilityLabel,
  barLabel,
} from "@/components/shared/absence-chart/absence-chart-model";
import { teamChartRows } from "./vacation-chart-model";
import { AbsenceChartFrame } from "@/components/shared/absence-chart/absence-chart-frame";
import { AbsenceChartLegend } from "@/components/shared/absence-chart/absence-chart-legend";
import { AbsenceChartRow } from "@/components/shared/absence-chart/absence-chart-row";
import type { AbsenceKind } from "@/lib/rs/absence-kinds";
import type { VacationExportView } from "./types";
import {
  resolveChartWidth,
  type ChartWidth,
} from "@/components/shared/absence-chart/absence-chart-width";

/**
 * The team vacation calendar: one row per employee with their vacations as bars over the days of the period,
 * approved ones solid, those in progress pale and dashed, and — if asked for — their availability entries: thin
 * lines beneath, not-available ones striped bars. The axis and rows are the vacation form's chart (AbsenceChartFrame,
 * AbsenceChartRow), its day columns as wide as chosen (`auto`: narrow beyond a quarter).
 *
 * The chart is the page's content, so it is its own text alternative: a group of the employees' names and their
 * bars, each focusable and named with its period and status (barLabel), the axis hidden from screen readers.
 *
 * Printed — by the print button or the browser's own command — it is laid out in the width of the page (see
 * AbsenceChartFrame), and always light: a dark theme would print as a black page now that the backgrounds are
 * printed (see globals.css).
 */
export function VacationTeamChart({
  view,
  dayWidth,
  absenceKinds,
}: {
  view: VacationExportView;
  dayWidth: ChartWidth;
  /** The absences shown, those of the view only (it may be the answer of the choice before, still shown). */
  absenceKinds: readonly AbsenceKind[];
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const rows = useMemo(
    () => teamChartRows(view, absenceKinds),
    [view, absenceKinds]
  );
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
      <AbsenceChartFrame
        days={view.days}
        ariaLabel={t("vacation.export.title")}
        dayWidth={resolveChartWidth(dayWidth, view.days.length)}
        role="group"
      >
        {rows.map((row, r) => (
          <AbsenceChartRow
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
      </AbsenceChartFrame>
      <AbsenceChartLegend
        team
        availabilities={absenceKinds.some((kind) => kind !== "VACATION")}
      />
    </figure>
  );
}
