"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import { logLevelRowClass } from "@/components/shared/log-level";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import { isAccessDenied } from "@/hooks/use-read-access-guard";
import {
  fetchAdminErrorDetail,
  fetchAdminErrorLogFile,
  type LogFileEvent,
} from "@/lib/rs/admin-errors";
import { LogFileFormatWarning } from "./log-file-format-warning";
import { logViewerColumns } from "./log-viewer-columns";

/**
 * The occurrences of a problem of the problem dashboard in the log files of its last days
 * (`/next/adminLogViewer?problem=<id>`), also of those before the server's start, which the log viewer
 * doesn't have any more. Searched once on opening (the backend reads the files on every request), with a
 * warning naming the format to configure if the files aren't fully readable. Admin group only.
 */
export function LogFileSearch({ problemId }: { problemId: number }) {
  const t = useTranslations();
  const { isAdmin, isLoading } = useAuth();
  const detail = useQuery({
    queryKey: ["adminErrors", "detail", problemId],
    queryFn: ({ signal }) => fetchAdminErrorDetail(problemId, signal),
    enabled: isAdmin,
  });
  const search = useQuery({
    queryKey: ["adminErrors", "logFile", problemId],
    queryFn: ({ signal }) => fetchAdminErrorLogFile(problemId, signal),
    enabled: isAdmin,
    staleTime: Infinity,
  });
  const columns = useMemo(() => logFileColumns(t), [t]);

  const entry = detail.data?.entry;
  const title = t("system.admin.adminErrors.logFile.title", {
    arg0: entry?.message || entry?.exceptionClass || entry?.code || problemId,
  });
  const denied = (!isLoading && !isAdmin) || isAccessDenied(search.error);
  const data = search.data;

  return (
    <PageShell>
      <PageTitleRow
        category={t("system.admin.adminErrors.title")}
        title={title}
      >
        <Button
          size="sm"
          variant="outline"
          disabled={search.isFetching}
          onClick={() => search.refetch()}
        >
          {t("refresh")}
        </Button>
      </PageTitleRow>
      <div className="space-y-2 px-4 pb-2 pt-3 text-sm">
        {denied ? (
          <p className="text-destructive">{t("access.exception.noAccess")}</p>
        ) : search.isError ? (
          <p className="text-destructive">{t("errorpage.title")}</p>
        ) : !data ? (
          <p className="text-muted-foreground">{t("loading")}</p>
        ) : (
          <>
            <LogFileFormatWarning data={data} />
            <p className="text-muted-foreground">
              {t("system.admin.adminErrors.logFile.files", {
                arg0: data.searchedFiles.join(", ") || "-",
              })}
            </p>
            {data.truncated && (
              <p className="text-muted-foreground">
                {t("system.admin.adminErrors.logFile.truncated")}
              </p>
            )}
            {data.entries.length === 0 && (
              <p className="text-muted-foreground">
                {t("system.admin.adminErrors.logFile.none")}
              </p>
            )}
          </>
        )}
      </div>
      {data && data.entries.length > 0 && (
        <div className="flex min-h-0 flex-1 flex-col px-4 pb-4">
          <DataTable<LogFileEvent>
            columns={columns}
            data={data.entries}
            isFetching={search.isFetching}
            enableColumnFilters={false}
            manualSorting={false}
            showPagination={false}
            dense
            getRowId={(row) => String(row.id)}
            rowClassName={(row) => logLevelRowClass(row.level)}
          />
        </div>
      )}
    </PageShell>
  );
}

/** The admin log viewer's columns, with where an entry was found instead of the user agent (not in the file). */
function logFileColumns(
  t: ReturnType<typeof useTranslations>
): ColumnDef<LogFileEvent, unknown>[] {
  const columns: ColumnDef<LogFileEvent, unknown>[] =
    logViewerColumns<LogFileEvent>(t, true).filter(
      (column) => column.id !== "userAgent"
    );
  columns.push({
    id: "source",
    header: t("system.admin.adminErrors.logFile.source"),
    size: 260,
    enableSorting: false,
    meta: { label: t("system.admin.adminErrors.logFile.source"), wrap: true },
    cell: ({ row }) => (
      <span className="break-all text-muted-foreground">
        {row.original.source}
        <br />
        {row.original.logger}
      </span>
    ),
  });
  return columns;
}
