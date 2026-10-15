"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { HRPlanningListStatistics } from "./types";

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
      className={cn(
        "flex flex-wrap items-baseline gap-x-4 gap-y-1 border-b bg-muted/40 px-4 py-1.5 text-[13px]",
        isFetching && "opacity-60"
      )}
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
