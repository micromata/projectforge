"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation, useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { PdfIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";
import {
  downloadMonthlyEmployeeReportPdf,
  fetchMonthlyEmployeeReport,
} from "@/lib/rs/monthly-employee-report";
import { ReportFilterRow } from "./report-filter-row";
import { ReportHeader } from "./report-header";
import { ReportMatrix } from "./report-matrix";
import { ReportTitleStats } from "./report-title-stats";
import type { MonthlyReportQuery } from "./types";

function initialNumber(
  params: URLSearchParams,
  key: string
): number | undefined {
  const value = Number(params.get(key));
  return value > 0 ? value : undefined;
}

/**
 * The monthly employee report ("Monatsbericht"), a hand-built standalone page (like the global search).
 *
 * The filter — user (only when the account may read other users' time sheets), year and month — drives a
 * single query keyed on that triple; the deep-link `?userId=&year=&month=` seeds it. The report arrives
 * fully computed and pre-formatted, so the matrix and the header only render it. Each matrix row drills
 * down into the filtered time sheet list (see ReportMatrix).
 */
export function MonthlyEmployeeReportPage() {
  const t = useTranslations();
  const params = useSearchParams();
  const [query, setQuery] = useState<MonthlyReportQuery>(() => ({
    userId: initialNumber(params, "userId"),
    year: initialNumber(params, "year"),
    month: initialNumber(params, "month"),
  }));

  const report = useQuery({
    queryKey: ["monthlyEmployeeReport", query],
    queryFn: ({ signal }) => fetchMonthlyEmployeeReport(query, signal),
  });
  const pdf = useMutation({
    mutationFn: () => downloadMonthlyEmployeeReportPdf(query),
  });

  const data = report.data;

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.monthlyEmployeeReport._")}
        title={t("menu.monthlyEmployeeReport._")}
      >
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => pdf.mutate()}
          disabled={pdf.isPending || !data}
        >
          <HugeiconsIcon icon={PdfIcon} size={14} aria-hidden />
          {t("exportAsPdf")}
        </Button>
        <LegacyPageLink url="wa/monthlyEmployeeReport?legacyEscape" />
      </PageTitleRow>
      <div className="flex flex-col gap-4 px-4 pb-6">
        {/* Filter (user / year / month) on the left, the key figures right-aligned on the same line. */}
        <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-4">
          <ReportFilterRow report={data} value={query} onChange={setQuery} />
          {data && <ReportTitleStats report={data} />}
        </div>
        {report.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {report.isError && (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        )}
        {data && (
          <>
            <ReportHeader report={data} />
            <ReportMatrix report={data} />
          </>
        )}
      </div>
    </PageShell>
  );
}
