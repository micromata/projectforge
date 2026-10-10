"use client";

import { useState } from "react";
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
import { ProblemTabContents } from "./problem-tab-contents";
import { SCHEDULER_QUERY_KEY } from "./scheduler-job-detail-dialog";
import { SchedulerTab } from "./scheduler-tab";
import {
  SYSTEM_STATISTICS_QUERY_KEY,
  SystemStatisticsTab,
} from "./system-statistics-tab";
import { useProblemDashboard } from "./use-problem-dashboard";

const ADMIN_TABS = ["overview", "problems", "scheduler", "statistics"];

/** Everybody may see the system statistics (non-admins only the key figures of the database). */
const USER_TABS = ["statistics"];

/**
 * The system dashboard (`/next/systemDashboard`). For the admin group the problems the log aggregation counted -
 * every collected error and warning, grouped -, their trends and status: the overview tab shows the key figures and
 * the state of the active subsystems, a key figure or a subsystem's tile shows its problems in the problems tab. A
 * problem's detail explains it and changes its status (acknowledge, resolve, ignore, mute), which also decides what
 * the error digest reports. The digest links each problem as `?id=<id>`, which opens its detail. The statistics tab
 * (`?tab=statistics`, the only one for other users) shows the system statistics, the scheduler tab
 * (`?tab=scheduler`) the scheduled jobs with their runs.
 */
export function SystemDashboard() {
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
  const ownData = statistics || tab === "scheduler";
  const data = problems.list.data;

  return (
    <PageShell>
      <PageTitleRow title={t("system.dashboard.title")}>
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
                    : "adminErrors",
              ],
            })
          }
        >
          {t("refresh")}
        </Button>
        {!ownData && (
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
            </>
          )}
          <TabsTrigger value="statistics">
            {t("system.statistics.title")}
          </TabsTrigger>
        </TabsList>
        {isAdmin && <ProblemTabContents problems={problems} setTab={setTab} />}
        {isAdmin && <SchedulerTab />}
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
