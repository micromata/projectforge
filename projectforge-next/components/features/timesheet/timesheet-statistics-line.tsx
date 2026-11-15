"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { leafKeyOf } from "@/lib/leaf-key";
import type { TimesheetStatistics } from "./timesheet-statistics";
import { STATISTICS_LINE_CLASS } from "@/components/shared/statistics-line-class";

/**
 * The footer of the time sheet list above its table: the summed duration and, where the installation
 * tracks it, the share of time saved by AI — the two values the legacy list shows in its footer
 * (`TimesheetEntityRest.postProcessResultSet`) — and, if there are any, the booked hours overlapping other
 * time sheets (shared costs) with their share of the total.
 *
 * The numbers are the backend's, computed over the whole result set of the same filter and already
 * formatted in the user's locale (see [TimesheetStatistics]); summing the loaded rows here would answer
 * differently once a page is scrolled and put the AI-savings rules into the browser a second time.
 */
export function TimesheetStatisticsLine({
  statistics,
  isFetching,
  className,
}: {
  statistics: TimesheetStatistics | undefined;
  /** Dims the line while a new result set is on its way, so a stale sum doesn't read as final. */
  isFetching?: boolean;
  className?: string;
}) {
  const t = useTranslations();
  if (!statistics) return null;

  return (
    <dl
      className={cn(
        STATISTICS_LINE_CLASS,
        isFetching && "opacity-60",
        className
      )}
      aria-label={t("statistics")}
    >
      <div className="flex items-baseline gap-1.5 text-brand-teal">
        <dt className="text-[11px] opacity-70">
          {t("timesheet.totalDuration")}
        </dt>
        <dd className="font-medium tabular-nums">{statistics.totalDuration}</dd>
      </div>
      {/* Only where the installation tracks AI time savings, as the backend decides per result set. */}
      {statistics.aiEnabled && statistics.aiPercentage && (
        <div className="flex items-baseline gap-1.5">
          <dt className="text-[11px] opacity-70">
            {t(leafKeyOf("timesheet.ai.timeSavedByAI", t.has))}
          </dt>
          <dd className="tabular-nums">{statistics.aiPercentage}</dd>
        </div>
      )}
      {/* Only if sheets of the result overlap others (shared costs); most never do. */}
      {statistics.sharedCostDuration && (
        <div className="flex items-baseline gap-1.5">
          <dt className="text-[11px] opacity-70">
            {t(leafKeyOf("fibu.kost2.sharedCost", t.has))}
          </dt>
          <dd className="tabular-nums">
            {statistics.sharedCostDuration} ({statistics.sharedCostPercentage})
          </dd>
        </div>
      )}
    </dl>
  );
}
