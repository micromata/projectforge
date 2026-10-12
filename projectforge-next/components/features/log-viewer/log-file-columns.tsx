"use client";

import type { ColumnDef } from "@tanstack/react-table";
import type { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  formatElapsed,
  formatTimestampMillis,
  type FormatContext,
} from "@/lib/format";
import type { LogFileEvent } from "@/lib/rs/admin-errors";
import { logViewerColumns } from "./log-viewer-columns";
import { highlightTerms } from "./log-file-text";
import { LoggerMenu, type OnLogger } from "./logger-menu";

type T = ReturnType<typeof useTranslations>;

/** The id of an entry of the log files: its file and first line. */
export const logFileRowId = (entry: LogFileEvent) =>
  `${entry.file}:${entry.line}`;

export interface LogFileColumnOptions {
  t: T;
  formatCtx: FormatContext;
  /** The words searched for, highlighted in the message and the stack trace. */
  terms: string[];
  onLogger?: OnLogger;
  /** The column with the button opening a hit's context; left out for the entries of a context. */
  context?: { isOpen: (id: string) => boolean; toggle: (id: string) => void };
  /** The time since the entry before, per row id (the context shows a jump in time). */
  gaps?: Map<string, number>;
}

/**
 * The columns of the entries of the log files: those of the admin log viewer (level, user) with the time to
 * the millisecond in the user's zone, the logger (with the actions narrowing the search) instead of the user
 * agent, which the file doesn't have, and the context of a hit.
 */
export function logFileColumns({
  t,
  formatCtx,
  terms,
  onLogger,
  context,
  gaps,
}: LogFileColumnOptions): ColumnDef<LogFileEvent, unknown>[] {
  const viewer = logViewerColumns<LogFileEvent>(t, true);
  const shared = (id: string) => viewer.find((column) => column.id === id)!;
  const columns: ColumnDef<LogFileEvent, unknown>[] = [
    {
      ...shared("timestamp"),
      size: 190,
      cell: ({ row }) => {
        const gap = gaps?.get(row.id);
        return (
          <div className="flex flex-col leading-tight">
            <span className="whitespace-nowrap tabular-nums">
              {formatTimestampMillis(row.original.timestampMillis, formatCtx)}
            </span>
            <span className="whitespace-nowrap text-xs text-muted-foreground">
              {gap != null && gap >= 1000
                ? `+ ${formatElapsed(gap, formatCtx)}`
                : row.original.timeAgo}
            </span>
          </div>
        );
      },
    },
    shared("level"),
    { ...shared("user"), size: 150 },
    {
      id: "logger",
      header: t("system.admin.logViewer.loggerName"),
      size: 240,
      enableSorting: false,
      meta: { label: t("system.admin.logViewer.loggerName"), wrap: true },
      cell: ({ row }) => (
        <LoggerMenu
          logger={row.original.logger}
          source={row.original.source}
          onLogger={onLogger}
        />
      ),
    },
    {
      ...shared("message"),
      size: 560,
      cell: ({ row }) => (
        <span className="whitespace-pre-wrap break-words">
          {highlightTerms(row.original.message ?? "", terms)}
        </span>
      ),
    },
    {
      ...shared("stackTrace"),
      size: 240,
      cell: ({ row }) =>
        row.original.stackTrace ? (
          <details>
            <summary className="cursor-pointer text-muted-foreground">
              {t("system.admin.logViewer.stacktrace")}
            </summary>
            <pre className="mt-1 max-h-96 overflow-auto whitespace-pre-wrap break-all font-mono text-[11px] leading-snug">
              {highlightTerms(row.original.stackTrace, terms)}
            </pre>
          </details>
        ) : null,
    },
  ];
  if (context) {
    columns.push({
      id: "context",
      header: t("system.admin.adminLogViewer.logFile.context"),
      size: 90,
      enableSorting: false,
      meta: { label: t("system.admin.adminLogViewer.logFile.context") },
      cell: ({ row }) => {
        const open = context.isOpen(row.id);
        return (
          <Button
            size="xs"
            variant={open ? "secondary" : "outline"}
            aria-expanded={open}
            onClick={() => context.toggle(row.id)}
          >
            {t("system.admin.adminLogViewer.logFile.context")}
          </Button>
        );
      },
    });
  }
  return columns;
}
