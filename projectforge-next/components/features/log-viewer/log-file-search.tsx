"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import { isAccessDenied } from "@/hooks/use-read-access-guard";
import {
  fetchAdminErrorDetail,
  fetchAdminErrorLogFile,
} from "@/lib/rs/admin-errors";
import { LogFileEntries } from "./log-file-entries";
import { LogFileFormatWarning } from "./log-file-format-warning";
import { LogSourceSwitch } from "./log-source-switch";
import { LogViewerShell, LogViewerTitle } from "./log-viewer-frame";

/** The entries before and after an occurrence its context starts with. */
const CONTEXT_SIZE = 10;

/**
 * The occurrences of a problem of the system dashboard in the log files of its last days
 * (`/next/systemDashboard?tab=logs&incident=<id>`), also of those before the server's start, which the log viewer
 * doesn't have any more. Searched once on opening (the backend reads the files on every request), with a
 * warning naming the format to configure if the files aren't fully readable, each occurrence with its context
 * (what happened before and after it). Admin group only.
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

  const entry = detail.data?.entry;
  const title = t("system.admin.adminErrors.logFile.title", {
    arg0: entry?.message || entry?.exceptionClass || entry?.code || problemId,
  });
  const denied = (!isLoading && !isAdmin) || isAccessDenied(search.error);
  const data = search.data;

  return (
    <LogViewerShell>
      <LogViewerTitle
        category={t("system.dashboard.title")}
        title={title}
        center={<LogSourceSwitch value="files" />}
        embeddedTitle
      >
        <Button
          size="sm"
          variant="outline"
          disabled={search.isFetching}
          onClick={() => search.refetch()}
        >
          {t("refresh")}
        </Button>
      </LogViewerTitle>
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
        <div className="min-h-0 flex-1 overflow-y-auto px-4 pb-4">
          <LogFileEntries
            entries={data.entries}
            before={CONTEXT_SIZE}
            after={CONTEXT_SIZE}
          />
        </div>
      )}
    </LogViewerShell>
  );
}
