"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import type { MonthlyReport } from "./types";

/** One inline label/value pair of the title-row statistics (label left, value right, baseline-aligned). */
function Stat({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-baseline gap-1.5">
      <dt className="text-[11px] opacity-70">{label}</dt>
      <dd className="text-sm tabular-nums">{children}</dd>
    </div>
  );
}

/**
 * The report's key figures repeated prominently in the page title row (top-right): working days, target
 * ("Soll") working hours and the net/gross totals — a compact right-aligned summary next to the actions,
 * mirroring the invoice edit page's sums line (invoice-sums-line.tsx). The fuller statistics stay in the
 * ReportHeader below; this line is the at-a-glance version.
 */
export function ReportTitleStats({ report }: { report: MonthlyReport }) {
  const t = useTranslations();
  return (
    <dl
      className="flex flex-wrap items-baseline justify-end gap-x-4 gap-y-1"
      aria-label={t("statistics")}
    >
      {report.numberOfWorkingDays && (
        <Stat label={t("fibu.common.workingDays")}>
          {report.numberOfWorkingDays}
        </Stat>
      )}
      {report.targetWorkingHours && (
        <Stat label={t("fibu.monthlyEmployeeReport.targetWorkingHours")}>
          {report.targetWorkingHours}
        </Stat>
      )}
      <Stat label={t("fibu.common.netto")}>{report.totalNetDuration}</Stat>
      {report.showGrossRow && (
        <Stat label={t("fibu.common.brutto")}>{report.totalGrossDuration}</Stat>
      )}
    </dl>
  );
}
