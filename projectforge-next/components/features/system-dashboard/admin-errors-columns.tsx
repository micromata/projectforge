"use client";

import type { ColumnDef } from "@tanstack/react-table";
import type { useTranslations } from "next-intl";
import { DataTableColumnHeader } from "@/components/data-table";
import type { FilterKind } from "@/components/data-table";
import { Sparkline } from "@/components/shared/chart/sparkline";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { LOG_LEVEL_KEYS, logLevelTone } from "@/components/shared/log-level";
import { StatusPill } from "@/components/shared/status-pill";
import {
  formatNumber,
  formatTimestampMinutes,
  type FormatContext,
} from "@/lib/format";
import type { LogGroupEntry } from "@/lib/rs/admin-errors";
import { AdminErrorStatus } from "./admin-error-status";
import { CATEGORY_KEYS } from "./admin-errors-labels";
import { statusText } from "./admin-errors-search";

type T = ReturnType<typeof useTranslations>;
type Column = ColumnDef<LogGroupEntry, unknown>;

/**
 * The columns of the system dashboard: when and how often a problem occurred (with its trend), what it is (code,
 * sample message, location) and its status. Sorted and filtered in the browser: the list holds all matching
 * problems. Status, level and category are filtered by their translated texts, as shown.
 */
export function adminErrorsColumns(t: T, ctx: FormatContext): Column[] {
  const header = (label: string, filterKind?: FilterKind): Column["header"] =>
    function AdminErrorsColumnHeader({ column, table }) {
      return (
        <DataTableColumnHeader
          column={column}
          table={table}
          filterKind={filterKind}
        >
          {label}
        </DataTableColumnHeader>
      );
    };
  const count = (value: number) => (
    <span className="tabular-nums">{formatNumber(value, ctx, 0)}</span>
  );
  const counter = (key: "count24h" | "totalCount", label: string): Column => ({
    id: key,
    accessorFn: (row) => row[key],
    header: header(label, "number"),
    size: key === "count24h" ? 70 : 80,
    meta: { label, align: "right" },
    cell: ({ row }) => count(row.original[key]),
  });
  return [
    {
      id: "lastSeen",
      accessorFn: (row) => row.lastSeen,
      header: header(t("system.admin.adminErrors.lastSeen")),
      size: 150,
      enableColumnFilter: false,
      meta: { label: t("system.admin.adminErrors.lastSeen") },
      cell: ({ row }) => (
        <span className="whitespace-nowrap tabular-nums">
          {formatTimestampMinutes(row.original.lastSeen, ctx)}
        </span>
      ),
    },
    {
      id: "status",
      accessorFn: (row) => statusText(row, t),
      header: header(t("status"), "text"),
      size: 150,
      meta: { label: t("status") },
      cell: ({ row }) => <AdminErrorStatus entry={row.original} />,
    },
    {
      id: "level",
      accessorFn: (row) => t(LOG_LEVEL_KEYS[row.level]),
      header: header(t("log.level._"), "text"),
      size: 90,
      meta: { label: t("log.level._") },
      cell: ({ row }) => (
        <StatusPill
          tone={logLevelTone(row.original.level)}
          label={t(LOG_LEVEL_KEYS[row.original.level])}
        />
      ),
    },
    {
      id: "category",
      accessorFn: (row) => t(CATEGORY_KEYS[row.category]),
      header: header(t("system.admin.adminErrors.category._"), "text"),
      size: 160,
      meta: { label: t("system.admin.adminErrors.category._"), wrap: true },
    },
    {
      id: "message",
      accessorFn: (row) => row.message || row.exceptionClass || row.code,
      header: header(t("system.admin.adminErrors.message"), "text"),
      size: 520,
      meta: { label: t("system.admin.adminErrors.message"), wrap: true },
      cell: ({ row, getValue, table }) => {
        const entry = row.original;
        const query = table.options.meta?.highlight;
        const origin = `${entry.code}${entry.location ? ` · ${entry.location}` : ""}`;
        return (
          <div className="min-w-0">
            <div className="line-clamp-2 break-words">
              <HighlightedText text={String(getValue())} query={query} />
            </div>
            <div className="truncate font-mono text-xs text-muted-foreground">
              <HighlightedText text={origin} query={query} />
            </div>
          </div>
        );
      },
    },
    counter("count24h", t("system.admin.adminErrors.count24h")),
    counter("totalCount", t("system.admin.adminErrors.totalCount")),
    {
      id: "trend",
      header: t("system.admin.adminErrors.trend"),
      size: 130,
      enableSorting: false,
      enableColumnFilter: false,
      meta: { label: t("system.admin.adminErrors.trend") },
      cell: ({ row }) => {
        const sum = row.original.trend.reduce((total, v) => total + v, 0);
        const label = `${t("system.admin.adminErrors.trend")}: ${formatNumber(sum, ctx, 0)}`;
        return (
          <HintTooltip plain text={label}>
            <span>
              <Sparkline values={row.original.trend} ariaLabel={label} />
            </span>
          </HintTooltip>
        );
      },
    },
  ];
}
