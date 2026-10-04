"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation, useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { PdfIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import {
  downloadMonthlyEmployeeReportPdf,
  fetchMonthlyEmployeeReport,
} from "@/lib/rs/monthly-employee-report";
import { useTabParam } from "@/hooks/use-tab-param";
import { InvoicingQuotaChartView } from "./invoicing-quota-chart-view";
import { ReportFilterRow } from "./report-filter-row";
import { ReportHeader } from "./report-header";
import { ReportMatrix } from "./report-matrix";
import { ReportTitleStats } from "./report-title-stats";
import { readQueryFromUrl, writeQueryToUrl } from "./report-url";
import type { MonthlyReportQuery } from "./types";

/**
 * The monthly employee report ("Monatsbericht"), a hand-built standalone page (like the global search).
 *
 * The filter — user (only when the account may read other users' time sheets), year and month — drives a
 * single query keyed on that triple; the deep-link `?userId=&year=&month=` seeds it and is kept up to date
 * with it, as is the open tab (`?tab=`, see useTabParam), so a reload shows the same report. The report arrives
 * fully computed and pre-formatted, so the matrix and the header only render it. Each matrix row drills
 * down into the filtered time sheet list (see ReportMatrix).
 */
export function MonthlyEmployeeReportPage() {
  const t = useTranslations();
  const params = useSearchParams();
  const [query, setQueryState] = useState<MonthlyReportQuery>(() =>
    readQueryFromUrl(params)
  );
  function setQuery(next: MonthlyReportQuery): void {
    setQueryState(next);
    writeQueryToUrl(next);
  }

  const report = useQuery({
    queryKey: ["monthlyEmployeeReport", query],
    queryFn: ({ signal }) => fetchMonthlyEmployeeReport(query, signal),
  });
  const pdf = useMutation({
    mutationFn: () => downloadMonthlyEmployeeReportPdf(query),
  });

  const data = report.data;
  // The quota tab exists only while the quota is shown; switching it off (or picking a user whose quota is
  // not visible) falls back to the report instead of leaving an empty tab selected. Undecided until the
  // report is there, so a deep link to the quota tab isn't dropped while it loads.
  const quotaTab = !!data?.invoicingQuotaAvailable && !!data.showInvoicingQuota;
  const [activeTab, setTab] = useTabParam(
    "report",
    data ? (quotaTab ? ["invoicingQuota"] : []) : undefined
  );

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
          <Tabs
            value={activeTab}
            onValueChange={setTab}
            className="flex flex-col gap-4"
          >
            {quotaTab && (
              <TabsList className="w-fit">
                <TabsTrigger value="report">
                  {t("fibu.monthlyEmployeeReport.tab.report")}
                </TabsTrigger>
                <TabsTrigger value="invoicingQuota">
                  {t("fibu.common.invoicingQuota._")}
                </TabsTrigger>
              </TabsList>
            )}
            <TabsContent value="report" className="flex flex-col gap-4">
              <ReportHeader report={data} />
              <ReportMatrix report={data} />
              {/* The average-working-time sentence is low-priority context, so it sits quietly below the table. */}
              {data.averageWorkingTimeStats && (
                <p className="text-xs text-muted-foreground">
                  <span className="opacity-70">{t("statistics")}:</span>{" "}
                  {data.averageWorkingTimeStats}
                </p>
              )}
            </TabsContent>
            {quotaTab && (
              <TabsContent value="invoicingQuota">
                <InvoicingQuotaChartView report={data} />
              </TabsContent>
            )}
          </Tabs>
        )}
      </div>
    </PageShell>
  );
}
