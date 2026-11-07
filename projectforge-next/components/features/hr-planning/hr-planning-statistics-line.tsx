"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { HRPlanningListStatistics } from "./types";
import { STATISTICS_LINE_CLASS } from "@/components/shared/statistics-line-class";

/**
 * The sum of the planned hours of the list, above its table — the "total duration" the legacy list showed
 * below its filter (`HRPlanningListForm`), summed by the backend over the rows of the filter
 * (`HRPlanningEntryEntityRest.postProcessResultSet`).
 */
export function HRPlanningStatisticsLine({
  statistics,
  isFetching,
}: {
  statistics: HRPlanningListStatistics | undefined;
  /** Dims the line while a new result set is on its way, so a stale sum doesn't read as final. */
  isFetching?: boolean;
}) {
  const t = useTranslations();
  const format = useFormatContext();
  if (!statistics) return null;

  return (
    <dl
      className={cn(STATISTICS_LINE_CLASS, isFetching && "opacity-60")}
      aria-label={t("statistics")}
    >
      <div className="flex items-baseline gap-1.5 text-brand-teal">
        <dt className="text-[11px] opacity-70">
          {t("timesheet.totalDuration")}
        </dt>
        <dd className="font-medium tabular-nums">
          {formatNumber(statistics.totalHours ?? 0, format, 2)}{" "}
          {t("hr.planning.hours")}
        </dd>
      </div>
    </dl>
  );
}
