"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import type { MonthlyReport } from "./types";

/** One labeled statistic in the header block. */
function Stat({
  label,
  children,
  title,
  danger,
}: {
  label: string;
  children: ReactNode;
  title?: string;
  danger?: boolean;
}) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
        {label}
      </span>
      <span
        className={danger ? "text-sm text-destructive" : "text-sm"}
        title={title}
      >
        {children}
      </span>
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
    <div className="grid grid-cols-2 gap-4 rounded-md border border-border bg-muted/30 p-4 sm:grid-cols-3 lg:grid-cols-4">
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
        <Stat label={t("statistics")}>{report.averageWorkingTimeStats}</Stat>
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
    </div>
  );
}
