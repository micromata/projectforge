"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import {
  keepPreviousData,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { ExportButton } from "@/components/shared/export-button";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/hooks/use-auth";
import { useExportDownload } from "@/hooks/use-export-download";
import { isAccessDenied } from "@/hooks/use-read-access-guard";
import {
  downloadAdminErrors,
  fetchAdminErrors,
  fetchAdminSubsystems,
  type LogGroupFilter,
} from "@/lib/rs/admin-errors";
import { AdminErrorDetailDialog } from "./admin-error-detail-dialog";
import { AdminErrorsOverview } from "./admin-errors-overview";
import { AdminErrorsProblems } from "./admin-errors-problems";

const START_FILTER: LogGroupFilter = { status: "OPEN", days: 7 };

/**
 * The problem dashboard (`/next/adminErrors`, admin group only): the problems the log aggregation counted - every
 * collected error and warning, grouped -, their trends and status. A problem's detail explains it and changes
 * its status (acknowledge, resolve, ignore, mute), which also decides what the error digest reports. The digest
 * links each problem as `?id=<id>`, which opens its detail. The overview tab shows the key figures and the state
 * of the active subsystems; a subsystem's tile shows its problems in the problems tab.
 */
export function AdminErrors() {
  const t = useTranslations();
  const { isAdmin, isLoading } = useAuth();
  const queryClient = useQueryClient();
  // Status, category, period and subsystem are the server's; the search works on the loaded problems (see
  // AdminErrorsTable), so typing doesn't fetch the list anew on every key.
  const [filter, setFilter] = useState<LogGroupFilter>(START_FILTER);
  const [search, setSearch] = useState("");
  // The error digest links a problem as `?id=<id>`: its detail opens at once, above the problems.
  const linkedId = Number(useSearchParams().get("id")) || null;
  const [detailId, setDetailId] = useState<number | null>(linkedId);
  const [tab, setTab] = useState(linkedId ? "problems" : "overview");
  const closeDetail = () => {
    setDetailId(null);
    // Drops the link's id, so that a reload doesn't open the detail again. The native History API, not
    // `router.replace`, as in the order statistics.
    const query = new URLSearchParams(window.location.search);
    if (!query.has("id")) return;
    query.delete("id");
    const search = query.toString();
    window.history.replaceState(
      null,
      "",
      search ? `?${search}` : window.location.pathname
    );
  };

  const list = useQuery({
    queryKey: ["adminErrors", "list", filter],
    queryFn: ({ signal }) => fetchAdminErrors(filter, signal),
    enabled: isAdmin,
    placeholderData: keepPreviousData,
  });
  const subsystems = useQuery({
    queryKey: ["adminErrors", "subsystems"],
    queryFn: ({ signal }) => fetchAdminSubsystems(signal),
    enabled: isAdmin,
  });
  const subsystemTitle = filter.subsystem
    ? (subsystems.data?.find((it) => it.id === filter.subsystem)?.title ??
      filter.subsystem)
    : null;
  // All problems of the filter and the search, not only the listed ones, as JSON for an analysis (e.g. by an AI).
  const download = useExportDownload(() =>
    downloadAdminErrors({ ...filter, search })
  );
  const denied = (!isLoading && !isAdmin) || isAccessDenied(list.error);
  const data = list.data;

  return (
    <PageShell>
      <PageTitleRow title={t("system.admin.adminErrors.title")}>
        <Button
          size="sm"
          variant="outline"
          disabled={!data || list.isFetching}
          onClick={() =>
            queryClient.invalidateQueries({ queryKey: ["adminErrors"] })
          }
        >
          {t("refresh")}
        </Button>
        <ExportButton
          variant="outline"
          label={t("system.admin.adminErrors.downloadJson._")}
          tooltip={t("system.admin.adminErrors.downloadJson.tooltip")}
          isPending={download.isPending}
          disabled={!data}
          onClick={() => download.mutate()}
        />
      </PageTitleRow>
      {!data ? (
        <div className="px-4 pb-8 pt-2 text-sm">
          {denied ? (
            <p className="text-destructive">{t("access.exception.noAccess")}</p>
          ) : list.isError ? (
            <p className="text-destructive">{t("errorpage.title")}</p>
          ) : (
            <p className="text-muted-foreground">{t("loading")}</p>
          )}
        </div>
      ) : (
        <Tabs
          value={tab}
          onValueChange={setTab}
          className="min-h-0 flex-1 px-4 pb-4 pt-3"
        >
          <TabsList>
            <TabsTrigger value="overview">
              {t("system.admin.adminErrors.tab.overview")}
            </TabsTrigger>
            <TabsTrigger value="problems">
              {t("system.admin.adminErrors.tab.problems")}
            </TabsTrigger>
          </TabsList>
          {!data.enabled && (
            <p className="text-sm text-destructive">
              {t("system.admin.adminErrors.disabled")}
            </p>
          )}
          <TabsContent value="overview" className="text-sm">
            <AdminErrorsOverview
              summary={data.summary}
              subsystems={subsystems.data}
              subsystemsError={subsystems.isError}
              onOpenSubsystem={(subsystem) => {
                setFilter({ ...filter, subsystem: subsystem.id });
                setTab("problems");
              }}
            />
          </TabsContent>
          <TabsContent
            value="problems"
            className="flex min-h-0 flex-col gap-3 text-sm"
          >
            <AdminErrorsProblems
              data={data}
              isFetching={list.isFetching}
              filter={filter}
              subsystemTitle={subsystemTitle}
              onFilterChange={setFilter}
              search={search}
              onSearchChange={setSearch}
              onOpen={(entry) => setDetailId(entry.id)}
            />
          </TabsContent>
        </Tabs>
      )}
      <AdminErrorDetailDialog id={detailId} onClose={closeDetail} />
    </PageShell>
  );
}
