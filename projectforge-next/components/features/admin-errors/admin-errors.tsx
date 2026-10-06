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
import { useTabParam } from "@/hooks/use-tab-param";
import { updateSearchParams } from "@/lib/search-params";
import {
  downloadAdminErrors,
  fetchAdminErrors,
  fetchAdminSubsystems,
  type LogGroupFilter,
  type LogGroupScope,
} from "@/lib/rs/admin-errors";
import { AdminErrorDetailDialog } from "./admin-error-detail-dialog";
import { AdminErrorsOverview } from "./admin-errors-overview";
import { AdminErrorsProblems } from "./admin-errors-problems";

const START_FILTER: LogGroupFilter = { status: "OPEN", days: 7 };

const TABS = ["overview", "problems"];

/** The subsystem of a tile, see AdminSubsystemTiles. */
const SUBSYSTEM_PARAM = "subsystem";

/** The scope of a key figure, see AdminErrorsSummary. */
const SCOPE_PARAM = "scope";

const SCOPES: readonly LogGroupScope[] = ["NEW_24H", "REGRESSION"];

/**
 * The problem dashboard (`/next/adminErrors`, admin group only): the problems the log aggregation counted - every
 * collected error and warning, grouped -, their trends and status. A problem's detail explains it and changes
 * its status (acknowledge, resolve, ignore, mute), which also decides what the error digest reports. The digest
 * links each problem as `?id=<id>`, which opens its detail. The overview tab shows the key figures and the state
 * of the active subsystems; a key figure or a subsystem's tile shows its problems in the problems tab.
 */
export function AdminErrors() {
  const t = useTranslations();
  const { isAdmin, isLoading } = useAuth();
  const queryClient = useQueryClient();
  const params = useSearchParams();
  // The error digest links a problem as `?id=<id>`: its detail opens at once, above the problems.
  const linkedId = Number(params.get("id")) || null;
  const [detailId, setDetailId] = useState<number | null>(linkedId);
  // Tab, subsystem and scope are kept in the url, so that the back button returns to the previous view. The tab a link
  // opened stays when its id is dropped.
  const [fallbackTab] = useState(linkedId ? "problems" : "overview");
  const [tab, setTab] = useTabParam(fallbackTab, TABS);
  const subsystem = params.get(SUBSYSTEM_PARAM) || null;
  const scope = SCOPES.find((it) => it === params.get(SCOPE_PARAM)) ?? null;
  // Status, category and period stay local: going back shouldn't undo every change of a select. All of them are
  // the server's; the search works on the loaded problems (see AdminErrorsTable), so typing doesn't fetch the list
  // anew on every key.
  const [localFilter, setFilter] = useState<LogGroupFilter>(START_FILTER);
  const filter: LogGroupFilter = { ...localFilter, subsystem, scope };
  const [search, setSearch] = useState("");
  const closeDetail = () => {
    setDetailId(null);
    // Drops the link's id, so that a reload doesn't open the detail again.
    if (params.has("id")) updateSearchParams({ id: null }, "replace");
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
  const subsystemTitle = subsystem
    ? (subsystems.data?.find((it) => it.id === subsystem)?.title ?? subsystem)
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
              onOpenSummary={(it) => {
                // The key figures count all problems: a subsystem chosen before is dropped.
                setFilter({
                  status: it.status,
                  category: it.category,
                  days: it.days,
                });
                setSearch("");
                setTab("problems", {
                  [SUBSYSTEM_PARAM]: null,
                  [SCOPE_PARAM]: it.scope ?? null,
                });
              }}
              onOpenSubsystem={(it) =>
                setTab("problems", {
                  [SUBSYSTEM_PARAM]: it.id,
                  [SCOPE_PARAM]: null,
                })
              }
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
              onRemoveSubsystem={() =>
                updateSearchParams({ [SUBSYSTEM_PARAM]: null }, "push")
              }
              onRemoveScope={() =>
                updateSearchParams({ [SCOPE_PARAM]: null }, "push")
              }
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
