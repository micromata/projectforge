"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { leafKeyOf } from "@/lib/leaf-key";
import type { TimesheetStatistics } from "./timesheet-statistics";

/**
 * The footer of the time sheet list above its table: the summed duration and, where the installation
 * tracks it, the share of time saved by AI — the two values the legacy list shows in its footer
 * (`TimesheetPagesRest.postProcessResultSet`).
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
        "flex flex-wrap items-baseline gap-x-4 gap-y-1 border-b bg-muted/40 px-4 py-1.5 text-[13px]",
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
    </dl>
  );
}
