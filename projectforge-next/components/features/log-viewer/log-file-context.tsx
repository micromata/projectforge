"use client";

import { useCallback } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import type { LogFileEvent } from "@/lib/rs/admin-errors";
import { fetchLogFileContext } from "@/lib/rs/log-file";
import { LogContext } from "./log-context";
import { logFileColumns, logFileRowId } from "./log-file-columns";
import type { OnLogger } from "./logger-menu";
import type { LogColumnLayout } from "./use-log-table";

/**
 * The context of a hit of the log files: the entries before and after it in its file (not continued in the
 * file of the day before or after).
 */
export function LogFileContext({
  hit,
  terms,
  before,
  after,
  excludeLoggers,
  onLogger,
  layout,
}: {
  hit: LogFileEvent;
  terms: string[];
  before: number;
  after: number;
  excludeLoggers: string[];
  onLogger?: OnLogger;
  layout: LogColumnLayout;
}) {
  const t = useTranslations();
  const formatCtx = useFormatContext();
  const columns = useCallback(
    (gaps: Map<string, number>) =>
      logFileColumns({ t, formatCtx, terms, onLogger, gaps }),
    [t, formatCtx, terms, onLogger]
  );
  return (
    <LogContext<LogFileEvent>
      // Started anew with a changed number of entries before or after.
      key={`${before}-${after}`}
      queryKey={[
        "logFile",
        "context",
        hit.file,
        hit.line,
        hit.timestampMillis,
        excludeLoggers,
      ]}
      fetch={(request, signal) =>
        fetchLogFileContext(
          {
            ...request,
            file: hit.file,
            line: hit.line,
            timestampMillis: hit.timestampMillis,
            user: hit.user ?? "",
            logger: hit.logger,
            excludeLoggers,
          },
          signal
        )
      }
      before={before}
      after={after}
      columns={columns}
      layout={layout}
      getRowId={logFileRowId}
      noOlder={t("system.admin.adminLogViewer.logFile.noOlder", {
        arg0: hit.file,
      })}
      noNewer={t("system.admin.adminLogViewer.logFile.noNewer", {
        arg0: hit.file,
      })}
      notFound={t("system.admin.adminLogViewer.logFile.notFound")}
    />
  );
}
