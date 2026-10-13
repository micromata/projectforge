"use client";

import { useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { ExportButton } from "@/components/shared/export-button";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/hooks/use-auth";
import { useTabParam } from "@/hooks/use-tab-param";
import { AdminErrorDetailDialog } from "./admin-error-detail-dialog";
import { CACHES_QUERY_KEY, CachesTab } from "./caches-tab";
import { ProblemTabContents } from "./problem-tab-contents";
import { SCHEDULER_QUERY_KEY } from "./scheduler-job-detail-dialog";
import { SchedulerTab } from "./scheduler-tab";
import {
  SYSTEM_STATISTICS_QUERY_KEY,
  SystemStatisticsTab,
} from "./system-statistics-tab";
import { useProblemDashboard } from "./use-problem-dashboard";

const ADMIN_TABS = [
  "overview",
  "problems",
  "scheduler",
  "caches",
  "logs",
  "statistics",
];

/** Everybody may see the system statistics (non-admins only the key figures of the database). */
const USER_TABS = ["statistics"];

/**
 * The system dashboard (`/next/systemDashboard`). For the admin group the problems the log aggregation counted -
 * every collected error and warning, grouped -, their trends and status: the overview tab shows the key figures and
 * the state of the active subsystems, a key figure or a subsystem's tile shows its problems in the problems tab. A
 * problem's detail explains it and changes its status (acknowledge, resolve, ignore, mute), which also decides what
 * the error digest reports. The digest links each problem as `?id=<id>`, which opens its detail. The statistics tab
 * (`?tab=statistics`, the only one for other users) shows the system statistics, the scheduler tab
 * (`?tab=scheduler`) the scheduled jobs with their runs, the caches tab (`?tab=caches`) the caches with their
 * refreshes, the log viewer tab (`?tab=logs`, see [logViewerTab]) the log events in memory and in the log files.
 *
 * @param logViewerTab The admin log viewer's tab content (a TabsContent of value `logs`), handed in by the route:
 * it is a feature of its own, which this one doesn't import.
 */
export function SystemDashboard({
  logViewerTab,
}: {
  logViewerTab?: ReactNode;
}) {
  const t = useTranslations();
  const { isAdmin, isLoading } = useAuth();
  const queryClient = useQueryClient();
  const problems = useProblemDashboard();
  // Tab, subsystem and scope are kept in the url, so that the back button returns to the previous view. The tab a
  // link opened stays when its id is dropped.
  const [linkedTab] = useState(problems.linkedId ? "problems" : "overview");
  const allowed = isLoading ? undefined : isAdmin ? ADMIN_TABS : USER_TABS;
  const [tab, setTab] = useTabParam(
    isLoading || isAdmin ? linkedTab : "statistics",
    allowed
  );
  const statistics = tab === "statistics";
  // Tabs with data of their own: their refresh doesn't wait for the problems.
  const ownData = statistics || tab === "scheduler" || tab === "caches";
  // The log viewer has its own refresh, which also reads a period anew.
  const logs = tab === "logs";
  const data = problems.list.data;

  return (
    <PageShell>
      <PageTitleRow title={t("system.dashboard.title")}>
        {!logs && (
          <Button
            size="sm"
            variant="outline"
            disabled={!ownData && (!data || problems.list.isFetching)}
            onClick={() =>
              queryClient.invalidateQueries({
                queryKey: [
                  statistics
                    ? SYSTEM_STATISTICS_QUERY_KEY
                    : tab === "scheduler"
                      ? SCHEDULER_QUERY_KEY
                      : tab === "caches"
                        ? CACHES_QUERY_KEY
                        : "adminErrors",
                ],
              })
            }
          >
            {t("refresh")}
          </Button>
        )}
        {!ownData && !logs && (
          <ExportButton
            variant="outline"
            label={t("system.admin.adminErrors.downloadJson._")}
            tooltip={t("system.admin.adminErrors.downloadJson.tooltip")}
            isPending={problems.download.isPending}
            disabled={!data}
            onClick={() => problems.download.mutate()}
          />
        )}
      </PageTitleRow>
      <Tabs
        value={tab}
        onValueChange={setTab}
        className="min-h-0 flex-1 px-4 pb-4 pt-3"
      >
        <TabsList>
          {isAdmin && (
            <>
              <TabsTrigger value="overview">
                {t("system.admin.adminErrors.tab.overview")}
              </TabsTrigger>
              <TabsTrigger value="problems">
                {t("system.admin.adminErrors.tab.problems")}
              </TabsTrigger>
              <TabsTrigger value="scheduler">
                {t("system.scheduler.tab")}
              </TabsTrigger>
              <TabsTrigger value="caches">{t("system.caches.tab")}</TabsTrigger>
              <TabsTrigger value="logs">
                {t("system.admin.adminLogViewer.tab")}
              </TabsTrigger>
            </>
          )}
          <TabsTrigger value="statistics">
            {t("system.statistics.title")}
          </TabsTrigger>
        </TabsList>
        {isAdmin && <ProblemTabContents problems={problems} setTab={setTab} />}
        {isAdmin && <SchedulerTab />}
        {isAdmin && <CachesTab />}
        {isAdmin && logViewerTab}
        <SystemStatisticsTab />
      </Tabs>
      {isAdmin && (
        <AdminErrorDetailDialog
          id={problems.detailId}
          onClose={problems.closeDetail}
        />
      )}
    </PageShell>
  );
}
