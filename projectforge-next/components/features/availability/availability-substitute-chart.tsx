"use client";

import { useMemo } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { AbsenceChartFrame } from "@/components/shared/absence-chart/absence-chart-frame";
import { AbsenceChartLegend } from "@/components/shared/absence-chart/absence-chart-legend";
import {
  availabilityLabel,
  buildAbsenceChart,
  type PersonRef,
} from "@/components/shared/absence-chart/absence-chart-model";
import { AbsenceChartRow } from "@/components/shared/absence-chart/absence-chart-row";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateInput } from "@/lib/date-parse";
import { cn } from "@/lib/utils";
import type { AvailabilityValues } from "./availability-schema";
import { useAvailabilityStats } from "./use-availability-stats";

/** A substitute as a row of the chart: by user, the substitutes are users. */
function person(user: PersonRef | null | undefined) {
  return user ? { key: String(user.id), name: user.displayName ?? "" } : null;
}

/**
 * The entry being edited and the absences of its substitutes overlapping it, as bars over the calendar days
 * (`chartDays`, the period with three days on either side): which substitute is away when, and on which days nobody
 * is on duty — as the vacation form's chart (VacationOverlapChart), with the same parts. Their vacations and
 * not-available entries count, their other availability entries (remote, reachable, …) are shown only. The entry
 * follows the form as it is typed; the substitutes' absences come with the stats. Nothing without substitutes.
 */
export function AvailabilitySubstituteChart({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const form = useEntityEditForm();
  const stats = useAvailabilityStats();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => state.values as AvailabilityValues
  );
  const days = useMemo(() => stats?.chartDays ?? [], [stats?.chartDays]);
  const rows = useMemo(
    () =>
      buildAbsenceChart(
        days,
        {
          name: values.user?.displayName ?? "",
          startDate: values.startDate,
          endDate: values.endDate,
          // Drawn solid: the entry has no status of approval.
          status: "APPROVED",
        },
        values.substitutes.map((u) => person(u)!),
        (stats?.substituteVacations ?? []).map((v) => ({
          ...v,
          person: person(v.employee?.user),
        })),
        (stats?.substituteAvailabilities ?? []).map((a) => ({
          ...a,
          person: person(a.user),
        }))
      ),
    [days, values, stats?.substituteVacations, stats?.substituteAvailabilities]
  );
  const withAvailabilities = rows.some(
    (row) => (row.availabilities?.length ?? 0) > 0
  );
  if (
    days.length === 0 ||
    values.substitutes.length === 0 ||
    !values.startDate ||
    !values.endDate
  ) {
    return null;
  }
  return (
    // `min-w-0`: a grid cell would otherwise grow to the chart's width instead of letting it scroll.
    <figure
      className={cn(
        "chart-panel min-w-0 space-y-2 rounded-md border p-3",
        className
      )}
    >
      <AbsenceChartFrame
        days={days}
        ariaLabel={t("availability.chart.ariaLabel")}
      >
        {rows.map((row, r) => (
          <AbsenceChartRow
            key={row.key}
            row={row}
            gridRow={r + 3}
            days={days}
            period={(bar) =>
              `${formatDateInput(bar.startDate, ctx)} – ${formatDateInput(bar.endDate, ctx)}`
            }
            availabilityText={(bar) =>
              availabilityLabel(row.name, bar, (iso) =>
                formatDateInput(iso, ctx)
              )
            }
          />
        ))}
      </AbsenceChartFrame>
      <AbsenceChartLegend
        availabilities={withAvailabilities}
        ownLabel={t("availability.chart.own")}
      />
    </figure>
  );
}
