"use client";

import { useCallback } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import {
  fetchLogMemoryContext,
  type LogViewerEvent,
} from "@/lib/rs/log-viewer";
import { LogContext } from "./log-context";
import { logMemoryColumns, logMemoryRowId } from "./log-memory-columns";
import type { LogColumnLayout } from "./use-log-table";

/** The size of the context an entry of the live table opens with. */
export const MEMORY_CONTEXT_SIZE = 10;

/**
 * The context of a log event in memory: the events before and after it of the last ones the server holds
 * (AdminLogViewerRest.memoryContext). Newer ones may come in, "newer" reads them.
 */
export function LogMemoryContext({
  event,
  layout,
}: {
  event: LogViewerEvent;
  layout: LogColumnLayout;
}) {
  const t = useTranslations();
  const formatCtx = useFormatContext();
  const columns = useCallback(
    (gaps: Map<string, number>) => logMemoryColumns({ t, formatCtx, gaps }),
    [t, formatCtx]
  );
  return (
    <LogContext<LogViewerEvent>
      queryKey={["logViewer", "context", event.id]}
      fetch={(request, signal) =>
        fetchLogMemoryContext(
          { ...request, id: event.id, excludeLoggers: [] },
          signal
        )
      }
      before={MEMORY_CONTEXT_SIZE}
      after={MEMORY_CONTEXT_SIZE}
      columns={columns}
      layout={layout}
      getRowId={logMemoryRowId}
      noOlder={t("system.admin.adminLogViewer.memory.noOlder")}
      noNewer={t("system.admin.adminLogViewer.memory.noNewer")}
      notFound={t("system.admin.adminLogViewer.memory.notFound")}
    />
  );
}
