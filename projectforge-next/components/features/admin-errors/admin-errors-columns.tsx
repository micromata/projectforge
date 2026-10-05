"use client";

import type { ColumnDef } from "@tanstack/react-table";
import type { useTranslations } from "next-intl";
import { Sparkline } from "@/components/shared/chart/sparkline";
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

type T = ReturnType<typeof useTranslations>;

/**
 * The columns of the error dashboard: when and how often a problem occurred (with its trend), what it is (code,
 * sample message, location) and its status. Sorted in the browser: the list holds all matching problems.
 */
export function adminErrorsColumns(
  t: T,
  ctx: FormatContext
): ColumnDef<LogGroupEntry, unknown>[] {
  const count = (value: number) => (
    <span className="tabular-nums">{formatNumber(value, ctx, 0)}</span>
  );
  return [
    {
      id: "lastSeen",
      accessorKey: "lastSeen",
      header: t("system.admin.adminErrors.lastSeen"),
      size: 150,
      meta: { label: t("system.admin.adminErrors.lastSeen") },
      cell: ({ row }) => (
        <span className="whitespace-nowrap tabular-nums">
          {formatTimestampMinutes(row.original.lastSeen, ctx)}
        </span>
      ),
    },
    {
      id: "status",
      accessorKey: "status",
      header: t("status"),
      size: 150,
      meta: { label: t("status") },
      cell: ({ row }) => <AdminErrorStatus entry={row.original} />,
    },
    {
      id: "level",
      accessorKey: "level",
      header: t("log.level"),
      size: 90,
      meta: { label: t("log.level") },
      cell: ({ row }) => (
        <StatusPill
          tone={logLevelTone(row.original.level)}
          label={t(LOG_LEVEL_KEYS[row.original.level])}
        />
      ),
    },
    {
      id: "category",
      accessorKey: "category",
      header: t("system.admin.adminErrors.category"),
      size: 160,
      meta: { label: t("system.admin.adminErrors.category"), wrap: true },
      cell: ({ row }) => t(CATEGORY_KEYS[row.original.category]),
    },
    {
      id: "message",
      accessorKey: "code",
      header: t("system.admin.adminErrors.message"),
      size: 520,
      meta: { label: t("system.admin.adminErrors.message"), wrap: true },
      cell: ({ row }) => {
        const entry = row.original;
        return (
          <div className="min-w-0">
            <div className="line-clamp-2 break-words">
              {entry.message || entry.exceptionClass || entry.code}
            </div>
            <div className="truncate font-mono text-xs text-muted-foreground">
              {entry.code}
              {entry.location ? ` · ${entry.location}` : ""}
            </div>
          </div>
        );
      },
    },
    {
      id: "count24h",
      accessorKey: "count24h",
      header: t("system.admin.adminErrors.count24h"),
      size: 70,
      meta: { label: t("system.admin.adminErrors.count24h"), align: "right" },
      cell: ({ row }) => count(row.original.count24h),
    },
    {
      id: "totalCount",
      accessorKey: "totalCount",
      header: t("system.admin.adminErrors.totalCount"),
      size: 80,
      meta: { label: t("system.admin.adminErrors.totalCount"), align: "right" },
      cell: ({ row }) => count(row.original.totalCount),
    },
    {
      id: "trend",
      header: t("system.admin.adminErrors.trend"),
      size: 130,
      enableSorting: false,
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
