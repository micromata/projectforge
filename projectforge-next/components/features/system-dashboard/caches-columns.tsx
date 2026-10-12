"use client";

import type { ColumnDef } from "@tanstack/react-table";
import type { useTranslations } from "next-intl";
import {
  DataTableColumnHeader,
  type FilterKind,
} from "@/components/data-table";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import {
  formatNumber,
  formatTimestampMinutes,
  type FormatContext,
} from "@/lib/format";
import type { CacheEntry } from "@/lib/rs/admin-caches";
import { cn } from "@/lib/utils";
import { CACHE_STATE_KEYS } from "./cache-labels";
import { CacheStatePill } from "./cache-state-pill";
import { formatDurationMillis } from "./scheduler-labels";

type T = ReturnType<typeof useTranslations>;
type Column = ColumnDef<CacheEntry, unknown>;

/**
 * The columns of the CachesTable, the fourth one (beside the last refresh) with the button refreshing the cache — or
 * terminating its refresh while one is running.
 */
export function cacheColumns(
  t: T,
  ctx: FormatContext,
  onRefresh: (cache: CacheEntry) => void,
  refreshing: string | undefined,
  onTerminate: (cache: CacheEntry) => void
): Column[] {
  const header = (label: string, filterKind?: FilterKind): Column["header"] =>
    function CacheColumnHeader({ column, table }) {
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
  const count = (value: number, alert: boolean) => (
    <span
      className={cn("tabular-nums", alert && value > 0 && "text-destructive")}
    >
      {formatNumber(value, ctx, 0)}
    </span>
  );
  const label = {
    cache: t("system.caches.column.cache"),
    lastRefresh: t("system.caches.column.lastRefresh"),
    duration: t("system.caches.column.duration"),
    expireTime: t("system.caches.column.expireTime"),
    refreshes: t("system.caches.column.refreshes"),
    failures: t("system.caches.column.failures"),
    error: t("system.caches.column.lastError"),
    refresh: t("system.caches.refresh._"),
    terminate: t("system.caches.terminate._"),
  };
  return [
    {
      id: "name",
      accessorFn: (row) => row.status.name,
      header: header(label.cache, "text"),
      size: 260,
      meta: { label: label.cache, wrap: true },
      cell: ({ row, table }) => {
        const { name, qualifiedName } = row.original.status;
        const query = table.options.meta?.highlight;
        return (
          <div className="min-w-0">
            <div className="break-words font-medium">
              <HighlightedText text={name} query={query} />
            </div>
            <div className="break-words text-xs text-muted-foreground">
              <HighlightedText text={qualifiedName} query={query} />
            </div>
          </div>
        );
      },
    },
    {
      id: "state",
      accessorFn: (row) => t(CACHE_STATE_KEYS[row.state]),
      header: header(t("status"), "text"),
      size: 130,
      meta: { label: t("status") },
      cell: ({ row }) => {
        const abandoned = row.original.status.abandonedThreads ?? 0;
        return (
          <div className="flex flex-col items-start gap-1">
            <CacheStatePill cache={row.original} />
            {abandoned > 0 && (
              <HintTooltip text={t("system.caches.abandoned.tooltip")}>
                <span className="text-xs text-destructive">
                  {t("system.caches.abandoned._", { arg0: abandoned })}
                </span>
              </HintTooltip>
            )}
          </div>
        );
      },
    },
    {
      id: "lastRefresh",
      accessorFn: (row) => row.status.lastRefreshStart ?? 0,
      header: header(label.lastRefresh),
      size: 140,
      enableColumnFilter: false,
      meta: { label: label.lastRefresh },
      // "5 minutes ago" above the date and time; sorted by the timestamp (accessorFn).
      cell: ({ row }) => {
        const { lastRefreshTimeAgo, status } = row.original;
        const start = status.lastRefreshStart;
        if (!start) return null;
        return (
          <div className="flex flex-col leading-tight">
            <span
              className={cn(
                "whitespace-nowrap",
                status.lastRefreshError && "text-destructive"
              )}
            >
              {lastRefreshTimeAgo || formatTimestampMinutes(start, ctx)}
            </span>
            {lastRefreshTimeAgo && (
              <span className="text-muted-foreground whitespace-nowrap text-xs tabular-nums">
                {formatTimestampMinutes(start, ctx)}
              </span>
            )}
          </div>
        );
      },
    },
    {
      id: "actions",
      header: () => null,
      size: 110,
      enableSorting: false,
      enableColumnFilter: false,
      meta: { label: label.refresh },
      cell: ({ row }) =>
        row.original.status.refreshInProgress ? (
          <HintTooltip text={t("system.caches.terminate.tooltip")}>
            <Button
              size="sm"
              variant="destructive"
              onClick={() => onTerminate(row.original)}
            >
              {label.terminate}
            </Button>
          </HintTooltip>
        ) : (
          <HintTooltip text={t("system.caches.refresh.tooltip")}>
            <Button
              size="sm"
              variant="outline"
              disabled={refreshing === row.original.id}
              onClick={() => onRefresh(row.original)}
            >
              {label.refresh}
            </Button>
          </HintTooltip>
        ),
    },
    {
      id: "duration",
      accessorFn: (row) => row.status.lastRefreshDurationMs ?? -1,
      header: header(label.duration),
      size: 90,
      enableColumnFilter: false,
      meta: { label: label.duration, align: "right" },
      cell: ({ row }) => (
        <span className="whitespace-nowrap tabular-nums">
          {formatDurationMillis(row.original.status.lastRefreshDurationMs, ctx)}
        </span>
      ),
    },
    {
      id: "expireTime",
      accessorFn: (row) => row.status.expireTimeMs,
      header: header(label.expireTime),
      size: 100,
      enableColumnFilter: false,
      meta: { label: label.expireTime, align: "right" },
      cell: ({ row }) => (
        <span className="whitespace-nowrap tabular-nums">
          {formatDurationMillis(row.original.status.expireTimeMs, ctx)}
        </span>
      ),
    },
    {
      id: "refreshes",
      accessorFn: (row) => row.status.refreshCount,
      header: header(label.refreshes, "number"),
      size: 90,
      meta: { label: label.refreshes, align: "right" },
      cell: ({ row }) => count(row.original.status.refreshCount, false),
    },
    {
      id: "failures",
      accessorFn: (row) => row.status.failureCount,
      header: header(label.failures, "number"),
      size: 90,
      meta: { label: label.failures, align: "right" },
      cell: ({ row }) => count(row.original.status.failureCount, true),
    },
    {
      id: "lastError",
      accessorFn: (row) => row.status.lastRefreshError ?? "",
      header: header(label.error, "text"),
      size: 260,
      meta: { label: label.error, wrap: true },
      cell: ({ row, table }) => (
        <span className="line-clamp-2 break-words text-xs text-destructive">
          <HighlightedText
            text={row.original.status.lastRefreshError ?? ""}
            query={table.options.meta?.highlight}
          />
        </span>
      ),
    },
  ];
}
