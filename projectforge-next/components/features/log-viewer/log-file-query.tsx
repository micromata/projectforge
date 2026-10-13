"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import { useFormatContext } from "@/hooks/use-format";
import { isAccessDenied } from "@/hooks/use-read-access-guard";
import {
  fetchLogFileInitial,
  queryLogFile,
  type LogFileQueryFilter,
} from "@/lib/rs/log-file";
import {
  LogFileQueryFilterBar,
  type OnFilterChange,
} from "./log-file-query-filter";
import { CUSTOM_PERIOD, logFileBounds } from "./log-file-period";
import { LogFileResults } from "./log-file-results";
import { LogSourceSwitch } from "./log-source-switch";
import { LogViewerShell, LogViewerTitle } from "./log-viewer-frame";

/**
 * The admin log viewer's search over the log files (`/next/systemDashboard?tab=logs&source=files`): by text, period,
 * level and logger category, also of the days before the server's start, each hit with its context. Starts
 * with the admin's last filter; a search runs only on demand, since the backend reads the files every time.
 */
export function LogFileQuery() {
  const t = useTranslations();
  const { isAdmin, isLoading } = useAuth();
  const initial = useQuery({
    queryKey: ["logFile", "initial"],
    queryFn: ({ signal }) => fetchLogFileInitial(signal),
    enabled: isAdmin,
    staleTime: Infinity,
  });
  const title = t("system.admin.adminLogViewer.title");
  const denied = (!isLoading && !isAdmin) || isAccessDenied(initial.error);
  return (
    <LogViewerShell>
      {initial.data ? (
        <LogFileQueryContent title={title} initial={initial.data} />
      ) : (
        <>
          <LogViewerTitle
            title={title}
            center={<LogSourceSwitch value="files" />}
          />
          <p className="px-4 pt-2 text-sm text-muted-foreground">
            {denied
              ? t("access.exception.noAccess")
              : initial.isError
                ? t("errorpage.title")
                : t("loading")}
          </p>
        </>
      )}
    </LogViewerShell>
  );
}

function LogFileQueryContent({
  title,
  initial,
}: {
  title: string;
  initial: LogFileQueryFilter;
}) {
  const t = useTranslations();
  const formatCtx = useFormatContext();
  const [filter, setFilter] = useState<LogFileQueryFilter>(initial);
  // The filter of the search shown, with its bounds read when it was started; null before the first one.
  const [submitted, setSubmitted] = useState<LogFileQueryFilter | null>(null);
  /** @param refresh The search shown again, with the newest entries, leaving the filter being edited alone. */
  const search = (next: LogFileQueryFilter, refresh = false) => {
    const bounds = logFileBounds(next.period, next.from, next.until, formatCtx);
    // A custom period starts from the bounds of the last preset, so switching to it doesn't empty them.
    if (!refresh && next.period !== CUSTOM_PERIOD)
      setFilter({ ...next, ...bounds });
    const query = { ...next, ...bounds };
    // The same query (a custom period) is cached under its key: read it again.
    if (JSON.stringify(query) === JSON.stringify(submitted)) result.refetch();
    else setSubmitted(query);
  };
  const onChange: OnFilterChange = (patch, rerun) => {
    const next = { ...filter, ...patch };
    setFilter(next);
    if (rerun && submitted) search(next);
  };
  const result = useQuery({
    queryKey: ["logFile", "query", submitted],
    queryFn: ({ signal }) => queryLogFile(submitted!, signal),
    enabled: submitted !== null,
    placeholderData: keepPreviousData,
    staleTime: Infinity,
  });

  return (
    <>
      <LogViewerTitle title={title} center={<LogSourceSwitch value="files" />}>
        <Button
          size="sm"
          variant="outline"
          disabled={!submitted || result.isFetching}
          onClick={() => submitted && search(submitted, true)}
        >
          {t("refresh")}
        </Button>
      </LogViewerTitle>
      <LogFileQueryFilterBar
        filter={filter}
        searching={result.isFetching}
        onChange={onChange}
        onSearch={() => search(filter)}
      />
      <LogFileResults
        submitted={submitted}
        result={result}
        before={filter.before}
        after={filter.after}
        onLogger={(pattern, mode) => {
          const key = mode === "include" ? "includeLoggers" : "excludeLoggers";
          if (!filter[key].includes(pattern)) {
            onChange({ [key]: [...filter[key], pattern] }, true);
          }
        }}
      />
    </>
  );
}
