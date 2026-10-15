"use client";

import type { ColumnDef } from "@tanstack/react-table";
import type { useTranslations } from "next-intl";
import { formatElapsed, type FormatContext } from "@/lib/format";
import type { LogViewerEvent } from "@/lib/rs/log-viewer";
import {
  beforeStackTrace,
  contextColumn,
  type ContextToggle,
} from "./log-context-column";
import { logViewerColumns } from "./log-viewer-columns";
import { LoggerMenu } from "./logger-menu";

type T = ReturnType<typeof useTranslations>;

/** The id of a log event in memory: its order number. */
export const logMemoryRowId = (event: LogViewerEvent) => String(event.id);

/**
 * The columns of the admin log viewer's live table: those of the log viewer with the logger after the user, the
 * jump in time from the entry before in a context ([gaps], instead of "5 minutes ago") and the button opening
 * the context of an entry ([context]).
 */
export function logMemoryColumns({
  t,
  formatCtx,
  context,
  gaps,
}: {
  t: T;
  formatCtx: FormatContext;
  context?: ContextToggle;
  gaps?: Map<string, number>;
}): ColumnDef<LogViewerEvent, unknown>[] {
  const columns = logViewerColumns(t, true).flatMap((column) => {
    if (column.id === "timestamp" && gaps) {
      return [
        {
          ...column,
          cell: ({ row }) => {
            const gap = gaps.get(row.id) ?? 0;
            return (
              <div className="flex flex-col leading-tight">
                <span className="whitespace-nowrap">
                  {gap >= 1000
                    ? `+ ${formatElapsed(gap, formatCtx)}`
                    : row.original.timeAgo}
                </span>
                <span className="whitespace-nowrap text-xs tabular-nums text-muted-foreground">
                  {row.original.timestamp}
                </span>
              </div>
            );
          },
        } as ColumnDef<LogViewerEvent, unknown>,
      ];
    }
    if (column.id === "user") {
      const label = t("system.admin.logViewer.loggerName");
      return [
        column,
        {
          id: "logger",
          header: label,
          size: 240,
          enableSorting: false,
          meta: { label, wrap: true },
          cell: ({ row }) => (
            <LoggerMenu logger={row.original.logger ?? ""} source="" />
          ),
        } as ColumnDef<LogViewerEvent, unknown>,
      ];
    }
    return [column];
  });
  return context
    ? beforeStackTrace(columns, contextColumn(t, context))
    : columns;
}
