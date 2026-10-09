"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { DataTable } from "@/components/data-table";
import {
  LOG_LEVEL_KEYS,
  logLevelRowClass,
  logLevelTone,
} from "@/components/shared/log-level";
import { StatusPill } from "@/components/shared/status-pill";
import type { ScriptLogEntry } from "@/lib/rs/script";

type Row = ScriptLogEntry & { index: number };

/**
 * What the script logged while running: when, how severe and what, oldest first — as the backend keeps
 * it for the user's running or last execution. Errors tint their row.
 */
export function ScriptLogTable({
  entries,
  isFetching,
}: {
  entries: readonly ScriptLogEntry[];
  isFetching?: boolean;
}) {
  const t = useTranslations();
  const rows = useMemo<Row[]>(
    () => entries.map((entry, index) => ({ ...entry, index })),
    [entries]
  );
  const columns = useMemo<ColumnDef<Row, unknown>[]>(
    () => [
      {
        id: "timestamp",
        header: t("time"),
        size: 110,
        enableSorting: false,
        meta: { label: t("time") },
        cell: ({ row }) => (
          <span className="whitespace-nowrap tabular-nums">
            {row.original.timestamp}
          </span>
        ),
      },
      {
        id: "level",
        header: t("log.level._"),
        size: 90,
        enableSorting: false,
        meta: { label: t("log.level._") },
        cell: ({ row }) => (
          <StatusPill
            tone={logLevelTone(row.original.level)}
            label={t(LOG_LEVEL_KEYS[row.original.level])}
          />
        ),
      },
      {
        id: "message",
        header: t("message.title"),
        size: 760,
        enableSorting: false,
        meta: { label: t("message.title"), wrap: true },
        cell: ({ row }) => (
          <span className="whitespace-pre-wrap break-words">
            {row.original.message}
          </span>
        ),
      },
    ],
    [t]
  );
  return (
    <DataTable
      columns={columns}
      data={rows}
      isFetching={isFetching}
      enableColumnFilters={false}
      manualSorting={false}
      showPagination={false}
      dense
      getRowId={(row) => String(row.index)}
      rowClassName={(row) => logLevelRowClass(row.level)}
    />
  );
}
