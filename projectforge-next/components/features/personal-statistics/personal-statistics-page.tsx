"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  keepPreviousData,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { InvoicingQuotaChartView } from "@/components/shared/invoicing-quota/invoicing-quota-chart-view";
import { InvoicingQuotaSwitch } from "@/components/shared/invoicing-quota/invoicing-quota-switch";
import { useTabParam } from "@/hooks/use-tab-param";
import { fetchPersonalStatistics } from "@/lib/rs/personal-statistics";
import { DisciplineCharts } from "./discipline-charts";

/**
 * The personal statistics page ("My statistics", `/next/personalStatistics`), successor of Wicket's
 * `wa/personalStatistics`. A standalone, read-only page for the logged-in user with two tabs:
 *
 *  - the two "timesheet discipline" charts of the last N days (cumulative target vs. booked working hours,
 *    and how quickly timesheets are booked against the target), each with the legend of its key figures;
 *  - the invoicing quota ("Fakturaquote") of the last 12 months — only while the quota is configured and
 *    the user's switch is on. The switch is the same user pref as the monthly report's.
 *
 * The open tab is kept in `?tab=` (see useTabParam).
 */
export function PersonalStatisticsPage() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  // Undefined until the user toggles: the backend then answers with the persisted choice.
  const [showQuota, setShowQuota] = useState<boolean | undefined>(undefined);
  const query = useQuery({
    queryKey: ["personalStatistics", showQuota ?? null],
    queryFn: ({ signal }) => fetchPersonalStatistics(showQuota, signal),
    placeholderData: keepPreviousData,
  });
  const data = query.data;

  function toggleQuota(checked: boolean): void {
    setShowQuota(checked);
    // The monthly report shares the switch; drop its cached reports so they don't show the old state (its
    // quota history doesn't depend on the switch and stays cached).
    void queryClient.invalidateQueries({
      queryKey: ["monthlyEmployeeReport"],
      predicate: (q) => q.queryKey[1] !== "invoicingQuotaHistory",
    });
  }

  // As in the monthly report: the quota tab exists only while the quota is shown, and is undecided until the
  // statistics are there, so a deep link to it isn't dropped while they load.
  const quotaTab = !!data?.invoicingQuotaAvailable && !!data.showInvoicingQuota;
  const [activeTab, setTab] = useTabParam(
    "discipline",
    data ? (quotaTab ? ["invoicingQuota"] : []) : undefined
  );

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.personalStatistics")}
        title={t("personal.statistics.title")}
      >
        {data?.invoicingQuotaAvailable && (
          <InvoicingQuotaSwitch
            id="personal-statistics-show-invoicing-quota"
            checked={data.showInvoicingQuota}
            info={data.invoicingQuotaInfo}
            onCheckedChange={toggleQuota}
          />
        )}
      </PageTitleRow>
      <div className="flex flex-col gap-8 px-4 pb-8 pt-2">
        {query.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {query.isError && (
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
                <TabsTrigger value="discipline">
                  {t("personal.statistics.tab.timeTracking")}
                </TabsTrigger>
                <TabsTrigger value="invoicingQuota">
                  {t("fibu.common.invoicingQuota._")}
                </TabsTrigger>
              </TabsList>
            )}
            <TabsContent value="discipline">
              <DisciplineCharts stats={data} />
            </TabsContent>
            {quotaTab && (
              <TabsContent value="invoicingQuota">
                <InvoicingQuotaChartView />
              </TabsContent>
            )}
          </Tabs>
        )}
      </div>
    </PageShell>
  );
}
