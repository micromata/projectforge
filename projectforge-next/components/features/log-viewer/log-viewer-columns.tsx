"use client";

import type { ColumnDef } from "@tanstack/react-table";
import type { useTranslations } from "next-intl";
import { StatusPill } from "@/components/shared/status-pill";
import type { LogViewerEvent } from "@/lib/rs/log-viewer";
import { LOG_LEVEL_KEYS, logLevelTone } from "./log-level";

type T = ReturnType<typeof useTranslations>;

/**
 * The columns of the log viewer. A user sees when, how severe and what - the admin additionally who (user@ip,
 * user agent) and the stack trace, collapsed so a long trace doesn't push all other entries out of sight.
 */
export function logViewerColumns(
  t: T,
  admin: boolean
): ColumnDef<LogViewerEvent, unknown>[] {
  const columns: ColumnDef<LogViewerEvent, unknown>[] = [
    {
      id: "timestamp",
      header: t("timestamp"),
      size: admin ? 200 : 150,
      enableSorting: false,
      meta: { label: t("timestamp") },
      cell: ({ row }) => (
        <span className="whitespace-nowrap tabular-nums">
          {row.original.timestamp}
        </span>
      ),
    },
    {
      id: "level",
      header: t("system.admin.logViewer.level"),
      size: 90,
      enableSorting: false,
      meta: { label: t("system.admin.logViewer.level") },
      cell: ({ row }) => (
        <StatusPill
          tone={logLevelTone(row.original.level)}
          label={t(LOG_LEVEL_KEYS[row.original.level])}
        />
      ),
    },
  ];
  if (admin) {
    columns.push({
      id: "user",
      header: t("system.admin.logViewer.user"),
      size: 180,
      enableSorting: false,
      meta: { label: t("system.admin.logViewer.user"), wrap: true },
      cell: ({ row }) => <span className="break-all">{row.original.user}</span>,
    });
  }
  columns.push({
    id: "message",
    header: t("system.admin.logViewer.message"),
    size: admin ? 520 : 760,
    enableSorting: false,
    meta: { label: t("system.admin.logViewer.message"), wrap: true },
    cell: ({ row }) => (
      <span className="whitespace-pre-wrap break-words">
        {row.original.message}
      </span>
    ),
  });
  if (admin) {
    columns.push(
      {
        id: "userAgent",
        header: t("system.admin.logViewer.userAgent"),
        size: 200,
        enableSorting: false,
        meta: { label: t("system.admin.logViewer.userAgent"), wrap: true },
        cell: ({ row }) => (
          <span className="break-all text-muted-foreground">
            {row.original.userAgent}
          </span>
        ),
      },
      {
        id: "stackTrace",
        header: t("system.admin.logViewer.stacktrace"),
        size: 320,
        enableSorting: false,
        meta: { label: t("system.admin.logViewer.stacktrace"), wrap: true },
        cell: ({ row }) =>
          row.original.stackTrace ? (
            <details>
              <summary className="cursor-pointer text-muted-foreground">
                {t("system.admin.logViewer.stacktrace")}
              </summary>
              <pre className="mt-1 max-h-96 overflow-auto whitespace-pre-wrap break-all font-mono text-[11px] leading-snug">
                {row.original.stackTrace}
              </pre>
            </details>
          ) : null,
      }
    );
  }
  return columns;
}
