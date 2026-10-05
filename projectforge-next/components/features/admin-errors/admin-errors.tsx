"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { ExportButton } from "@/components/shared/export-button";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import { useExportDownload } from "@/hooks/use-export-download";
import { isAccessDenied } from "@/hooks/use-read-access-guard";
import {
  downloadAdminErrors,
  fetchAdminErrors,
  type LogGroupFilter,
} from "@/lib/rs/admin-errors";
import { AdminErrorDetailDialog } from "./admin-error-detail-dialog";
import { AdminErrorsFilters } from "./admin-errors-filters";
import { AdminErrorsSummary } from "./admin-errors-summary";
import { AdminErrorsTable } from "./admin-errors-table";

const START_FILTER: LogGroupFilter = { status: "OPEN", days: 7 };

/**
 * The problem dashboard (`/next/adminErrors`, admin group only): the problems the log aggregation counted - every
 * collected error and warning, grouped -, their trends and status. A problem's detail explains it and changes
 * its status (acknowledge, resolve, ignore, mute), which also decides what the error digest reports. The digest
 * links each problem as `?id=<id>`, which opens its detail.
 */
export function AdminErrors() {
  const t = useTranslations();
  const { isAdmin, isLoading } = useAuth();
  // Status, category and period are the server's; the search works on the loaded problems (see
  // AdminErrorsTable), so typing doesn't fetch the list anew on every key.
  const [filter, setFilter] = useState<LogGroupFilter>(START_FILTER);
  const [search, setSearch] = useState("");
  // The error digest links a problem as `?id=<id>`: its detail opens at once.
  const linkedId = Number(useSearchParams().get("id")) || null;
  const [detailId, setDetailId] = useState<number | null>(linkedId);
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
          onClick={() => list.refetch()}
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
        <>
          <div className="space-y-3 px-4 pb-2 pt-3">
            {!data.enabled && (
              <p className="text-sm text-destructive">
                {t("system.admin.adminErrors.disabled")}
              </p>
            )}
            <AdminErrorsSummary summary={data.summary} />
            <AdminErrorsFilters filter={filter} onChange={setFilter} />
            {data.total > data.entries.length && (
              <p className="text-sm text-muted-foreground">
                {t("system.admin.adminErrors.more", {
                  arg0: data.entries.length,
                  arg1: data.total,
                })}
              </p>
            )}
          </div>
          <div className="flex min-h-0 flex-1 flex-col px-4 pb-4">
            {data.entries.length === 0 ? (
              <p className="py-4 text-sm text-muted-foreground">
                {t("system.admin.adminErrors.none")}
              </p>
            ) : (
              <AdminErrorsTable
                entries={data.entries}
                isFetching={list.isFetching}
                search={search}
                onSearchChange={setSearch}
                onOpen={(entry) => setDetailId(entry.id)}
              />
            )}
          </div>
        </>
      )}
      <AdminErrorDetailDialog id={detailId} onClose={closeDetail} />
    </PageShell>
  );
}
