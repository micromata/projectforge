"use client";

import { useMemo, useState } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import {
  DataTable,
  DataTableColumnHeader,
  DataTableColumnPanel,
  useColumnStatePersistence,
  useDataTable,
  useStoredColumnState,
  useTableState,
  type ColumnState,
  type FilterKind,
} from "@/components/data-table";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { SearchInput } from "@/components/shared/list/search-input";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatNumber,
  formatTimestampMinutes,
  type FormatContext,
} from "@/lib/format";
import type { SchedulerJobEntry } from "@/lib/rs/admin-scheduler";
import { cn } from "@/lib/utils";
import { SchedulerJobStatus } from "./scheduler-job-status";
import {
  formatDurationMillis,
  JOB_STATUS_KEYS,
  scheduleText,
} from "./scheduler-labels";

type T = ReturnType<typeof useTranslations>;
type Column = ColumnDef<SchedulerJobEntry, unknown>;

/** The user prefs of the table (`AdminSchedulerRest.columnStates` / `setColumnStates`). */
const GRID = "adminScheduler";

interface Props {
  jobs: SchedulerJobEntry[];
  isFetching: boolean;
  onOpen: (job: SchedulerJobEntry) => void;
}

/**
 * The scheduled jobs: what they do, when they ran and run next, how long they took and how often they failed in
 * the last 7 days. Sorted by area at first; searchable over the texts, sortable and filterable per column, its
 * columns reorderable, pinnable and hideable, stored in the user's prefs. Rendered once the stored state has
 * arrived (or failed), so the columns don't jump from the default layout to the user's one. A click opens the
 * job's detail ([onOpen]).
 */
export function SchedulerJobsTable(props: Props) {
  const stored = useStoredColumnState(GRID);
  if (stored.isPending) return null;
  return <LoadedTable {...props} storedState={stored.data ?? {}} />;
}

function LoadedTable({
  jobs,
  isFetching,
  onOpen,
  storedState,
}: Props & { storedState: ColumnState }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const [search, setSearch] = useState("");
  const columns = useMemo(() => schedulerColumns(t, ctx), [t, ctx]);
  const searched = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return jobs;
    return jobs.filter((job) =>
      [
        job.id,
        job.title,
        job.description,
        job.areaTitle,
        t(JOB_STATUS_KEYS[job.status]),
        job.inactiveReason,
        job.lastError,
      ].some((text) => text?.toLowerCase().includes(term))
    );
  }, [jobs, search, t]);
  const state = useTableState({ restoredState: storedState });
  const table = useDataTable<SchedulerJobEntry>({
    columns,
    data: searched,
    sorting: state.sorting,
    onSortingChange: state.setSorting,
    columnFilters: state.columnFilters,
    onColumnFiltersChange: state.setColumnFilters,
    columnVisibility: state.columnVisibility,
    onColumnVisibilityChange: state.setColumnVisibility,
    columnPinning: state.columnPinning,
    onColumnPinningChange: state.setColumnPinning,
    columnSizing: state.columnSizing,
    onColumnSizingChange: state.setColumnSizing,
    columnOrder: state.columnOrder,
    onColumnOrderChange: state.setColumnOrder,
    enableColumnFilters: true,
    enableColumnResizing: true,
    manualPagination: true,
    getRowId: (row) => row.id,
    highlight: search,
  });
  useColumnStatePersistence(GRID, {
    sorting: state.sorting,
    columnVisibility: state.columnVisibility,
    columnPinning: state.columnPinning,
    columnSizing: state.columnSizing,
    columnOrder: state.columnOrder,
  });
  // Back to the columns as declared; the persistence then stores the empty state.
  const resetColumns = () => {
    state.setSorting([]);
    state.setColumnVisibility({});
    state.setColumnPinning({});
    state.setColumnSizing({});
    state.setColumnOrder([]);
    state.setColumnFilters([]);
  };
  return (
    <>
      <div className="flex items-center gap-2 pb-2">
        <div className="relative w-full max-w-md">
          <SearchInput value={search} onChange={setSearch} />
        </div>
        <DataTableColumnPanel table={table} onReset={resetColumns} />
      </div>
      <DataTable<SchedulerJobEntry>
        table={table}
        columns={[]}
        data={[]}
        isFetching={isFetching}
        showPagination={false}
        dense
        emptyState={t("nothingFound")}
        onRowClick={onOpen}
      />
    </>
  );
}

function schedulerColumns(t: T, ctx: FormatContext): Column[] {
  const header = (label: string, filterKind?: FilterKind): Column["header"] =>
    function SchedulerColumnHeader({ column, table }) {
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
  const time = (value: number | null | undefined, alert = false) => (
    <span
      className={cn(
        "whitespace-nowrap tabular-nums",
        alert && "text-destructive"
      )}
    >
      {value ? formatTimestampMinutes(value, ctx) : ""}
    </span>
  );
  const count = (value: number, alert: boolean) => (
    <span
      className={cn("tabular-nums", alert && value > 0 && "text-destructive")}
    >
      {formatNumber(value, ctx, 0)}
    </span>
  );
  const label = {
    area: t("system.scheduler.column.area"),
    job: t("system.scheduler.column.job"),
    schedule: t("system.scheduler.column.schedule"),
    lastRun: t("system.scheduler.column.lastRun"),
    duration: t("system.scheduler.column.duration"),
    nextRun: t("system.scheduler.column.nextRun"),
    runs: t("system.scheduler.column.runs7d"),
    errors: t("system.scheduler.column.errors7d"),
    avg: t("system.scheduler.column.avgDuration7d"),
  };
  return [
    {
      id: "area",
      accessorFn: (row) => row.areaTitle,
      header: header(label.area, "text"),
      size: 120,
      meta: { label: label.area },
    },
    {
      id: "title",
      accessorFn: (row) => row.title,
      header: header(label.job, "text"),
      size: 360,
      meta: { label: label.job, wrap: true },
      cell: ({ row, table }) => {
        const job = row.original;
        const query = table.options.meta?.highlight;
        return (
          <div className="min-w-0">
            <div className="break-words font-medium">
              <HighlightedText text={job.title} query={query} />
            </div>
            {job.description && (
              <div className="line-clamp-2 break-words text-xs text-muted-foreground">
                <HighlightedText text={job.description} query={query} />
              </div>
            )}
          </div>
        );
      },
    },
    {
      id: "status",
      accessorFn: (row) => t(JOB_STATUS_KEYS[row.status]),
      header: header(t("status"), "text"),
      size: 120,
      meta: { label: t("status") },
      cell: ({ row }) => <SchedulerJobStatus job={row.original} />,
    },
    {
      id: "schedule",
      accessorFn: (row) => scheduleText(row, t, ctx),
      header: header(label.schedule, "text"),
      size: 170,
      meta: { label: label.schedule, wrap: true },
      cell: ({ getValue }) => (
        <span className="break-words font-mono text-xs">
          {String(getValue())}
        </span>
      ),
    },
    {
      id: "lastRun",
      accessorFn: (row) => row.lastRun ?? 0,
      header: header(label.lastRun),
      size: 140,
      enableColumnFilter: false,
      meta: { label: label.lastRun },
      cell: ({ row }) =>
        time(row.original.lastRun, row.original.lastStatus === "ERROR"),
    },
    {
      id: "lastDuration",
      accessorFn: (row) => row.lastDurationMs ?? -1,
      header: header(label.duration),
      size: 90,
      enableColumnFilter: false,
      meta: { label: label.duration, align: "right" },
      cell: ({ row }) => (
        <span className="whitespace-nowrap tabular-nums">
          {formatDurationMillis(row.original.lastDurationMs, ctx)}
        </span>
      ),
    },
    {
      id: "nextRun",
      accessorFn: (row) => row.nextRun ?? Number.MAX_SAFE_INTEGER,
      header: header(label.nextRun),
      size: 140,
      enableColumnFilter: false,
      meta: { label: label.nextRun },
      cell: ({ row }) => time(row.original.nextRun, row.original.overdue),
    },
    {
      id: "runs7d",
      accessorFn: (row) => row.runs7d,
      header: header(label.runs, "number"),
      size: 80,
      meta: { label: label.runs, align: "right" },
      cell: ({ row }) => count(row.original.runs7d, false),
    },
    {
      id: "errors7d",
      accessorFn: (row) => row.errors7d,
      header: header(label.errors, "number"),
      size: 80,
      meta: { label: label.errors, align: "right" },
      cell: ({ row }) => count(row.original.errors7d, true),
    },
    {
      id: "avgDuration7d",
      accessorFn: (row) => row.avgDurationMs7d ?? -1,
      header: header(label.avg),
      size: 100,
      enableColumnFilter: false,
      meta: { label: label.avg, align: "right" },
      cell: ({ row }) => (
        <span className="whitespace-nowrap tabular-nums">
          {formatDurationMillis(row.original.avgDurationMs7d, ctx)}
        </span>
      ),
    },
  ];
}
