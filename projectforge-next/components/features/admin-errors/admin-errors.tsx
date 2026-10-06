"use client";

import { useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { DataTable } from "@/components/data-table";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import { isAccessDenied } from "@/hooks/use-read-access-guard";
import {
  fetchAdminErrors,
  type LogGroupEntry,
  type LogGroupFilter,
} from "@/lib/rs/admin-errors";
import { AdminErrorDetailDialog } from "./admin-error-detail-dialog";
import { adminErrorsColumns } from "./admin-errors-columns";
import { AdminErrorsFilters } from "./admin-errors-filters";
import { AdminErrorsSummary } from "./admin-errors-summary";

const START_FILTER: LogGroupFilter = { status: "OPEN", days: 7, search: "" };

/**
 * The error dashboard (`/next/adminErrors`, admin group only): the problems the log aggregation counted - every
 * collected error and warning, grouped -, their trends and status. A problem's detail explains it and changes
 * its status (acknowledge, resolve, ignore, mute), which also decides what the error digest reports. The digest
 * links each problem as `?id=<id>`, which opens its detail.
 */
export function AdminErrors() {
  const t = useTranslations();
  const ctx = useFormatContext();
  const { isAdmin, isLoading } = useAuth();
  const [filter, setFilter] = useState<LogGroupFilter>(START_FILTER);
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
  const columns = useMemo(() => adminErrorsColumns(t, ctx), [t, ctx]);
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
                  arg0: formatNumber(data.entries.length, ctx, 0),
                  arg1: formatNumber(data.total, ctx, 0),
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
              <DataTable<LogGroupEntry>
                columns={columns}
                data={data.entries}
                isFetching={list.isFetching}
                enableColumnFilters={false}
                manualSorting={false}
                showPagination={false}
                dense
                getRowId={(row) => String(row.id)}
                onRowClick={(row) => setDetailId(row.id)}
              />
            )}
          </div>
        </>
      )}
      <AdminErrorDetailDialog id={detailId} onClose={closeDetail} />
    </PageShell>
  );
}
