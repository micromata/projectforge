"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import type { MonthlyReport } from "./types";

/**
 * One labeled statistic in the header line: label above value, small and quiet, matching the app-wide
 * statistics-line convention (see order-sums-line.tsx) rather than a heavy KPI tile.
 */
function Stat({
  label,
  children,
  title,
  danger,
  className,
}: {
  label: string;
  children: ReactNode;
  title?: string;
  danger?: boolean;
  /** Extra classes on the pair wrapper, e.g. a max width so a long value wraps instead of widening the row. */
  className?: string;
}) {
  return (
    <div className={cn("flex flex-col", className)}>
      <dt className="text-[11px] opacity-70">{label}</dt>
      <dd
        className={cn("text-sm tabular-nums", danger && "text-destructive")}
        title={title}
      >
        {children}
      </dd>
    </div>
  );
}

/**
 * The report's header statistics: user, Kost1, working days, unbooked days, the average-working-time line,
 * the invoicing quota (Fakturaquote) and the vacation figures — each shown only when it applies, mirroring
 * the legacy page's conditional fieldsets.
 */
export function ReportHeader({ report }: { report: MonthlyReport }) {
  const t = useTranslations();
  return (
    <dl className="flex flex-wrap gap-x-6 gap-y-2" aria-label={t("statistics")}>
      {!report.maySelectOtherUsers && (
        <Stat label={t("timesheet.user")}>{report.userName}</Stat>
      )}
      {report.costConfigured && report.kost1 && (
        <Stat label={t("fibu.kost1._")}>{report.kost1}</Stat>
      )}
      {report.numberOfWorkingDays && (
        <Stat label={t("fibu.common.workingDays")}>
          {report.numberOfWorkingDays}
        </Stat>
      )}
      {report.formattedUnbookedDays && (
        <Stat label={t("fibu.monthlyEmployeeReport.withoutTimesheets")} danger>
          {report.formattedUnbookedDays}
        </Stat>
      )}
      {report.averageWorkingTimeStats && (
        // Capped width so the backend's full sentence wraps onto a few lines instead of stretching the row.
        <Stat label={t("statistics")} className="max-w-sm">
          {report.averageWorkingTimeStats}
        </Stat>
      )}
      {report.invoicingQuota && (
        <Stat
          label={t("fibu.common.invoicingQuota._")}
          title={report.invoicingQuotaTooltip ?? undefined}
        >
          {report.invoicingQuota}
        </Stat>
      )}
      {report.vacationAvailable && report.vacationCount && (
        <Stat label={t("vacation.annualleave")}>{report.vacationCount}</Stat>
      )}
      {report.vacationAvailable && report.vacationPlannedCount && (
        <Stat label={t("vacation.plandannualleave")}>
          {report.vacationPlannedCount}
        </Stat>
      )}
    </dl>
  );
}
