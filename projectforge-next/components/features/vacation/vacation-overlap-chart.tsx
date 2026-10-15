"use client";

import { useMemo } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateInput } from "@/lib/date-parse";
import { cn } from "@/lib/utils";
import { availabilityLabel, buildVacationChart } from "./vacation-chart-model";
import { VacationChartFrame } from "./vacation-chart-frame";
import { VacationChartLegend } from "./vacation-chart-legend";
import { VacationChartRow } from "./vacation-chart-row";
import type { VacationValues } from "./vacation-schema";
import { useVacationStats } from "./use-vacation-stats";

/**
 * The vacation being edited and the vacations of its substitutes overlapping it, as bars over the
 * calendar days (`chartDays`, the period with three days on either side): which substitute is away when,
 * and on which days nobody is on duty. Their availability entries too: a not-available one counts as a vacation,
 * the others (remote, reachable, …) are shown only. The vacation itself follows the form as it is typed; the
 * substitutes' come with the stats. Shown among the form's fields, above the comment; the section "vacations of
 * the replacements" holds the same vacations with all their details.
 */
export function VacationOverlapChart({ className }: { className?: string }) {
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
        stats?.conflictingVacations ?? [],
        stats?.replacementAvailabilities ?? []
      ),
    [
      days,
      values,
      stats?.conflictingVacations,
      stats?.replacementAvailabilities,
    ]
  );
  const withAvailabilities = rows.some(
    (row) => (row.availabilities?.length ?? 0) > 0
  );
  if (days.length === 0 || !values.startDate || !values.endDate) return null;

  return (
    // `min-w-0`: a grid cell would otherwise grow to the chart's width instead of letting it scroll. Set off
    // from the fields around it by a faint box — faint, so the grey of the weekends still shows in it.
    <figure
      className={cn(
        "chart-panel min-w-0 space-y-2 rounded-md border p-3",
        className
      )}
    >
      <VacationChartFrame days={days} ariaLabel={t("vacation.chart.ariaLabel")}>
        {rows.map((row, r) => (
          <VacationChartRow
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
      </VacationChartFrame>
      <VacationChartLegend availabilities={withAvailabilities} />
    </figure>
  );
}
