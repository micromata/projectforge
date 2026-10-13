"use client";

import { useMemo, useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import {
  DataTable,
  DataTableColumnPanel,
  useColumnStatePersistence,
  useDataTable,
  useStoredColumnState,
  useTableState,
  type ColumnState,
} from "@/components/data-table";
import { SearchInput } from "@/components/shared/list/search-input";
import { useFormatContext } from "@/hooks/use-format";
import type { CacheEntry } from "@/lib/rs/admin-caches";
import { CACHE_STATE_KEYS, cacheStateRank } from "./cache-labels";
import { cacheColumns } from "./caches-columns";

/** The user prefs of the table (`AdminCachesRest.columnStates` / `setColumnStates`). */
const GRID = "adminCaches";

interface Props {
  caches: CacheEntry[];
  isFetching: boolean;
  /** The id of the cache whose refresh is being started. */
  refreshing?: string;
  onRefresh: (cache: CacheEntry) => void;
  /** Terminates the running refresh of the cache (asks first). */
  onTerminate: (cache: CacheEntry) => void;
  /** Beside the search, e.g. "refresh all". */
  toolbar?: ReactNode;
}

/**
 * The caches: their state, when they were refreshed last, how long it took, after which time they expire and how
 * often a refresh failed since the start. The problems first, then by name; searchable, sortable and filterable,
 * its columns stored in the user's prefs (rendered once the stored state has arrived, see SchedulerJobsTable).
 */
export function CachesTable(props: Props) {
  const stored = useStoredColumnState(GRID);
  if (stored.isPending) return null;
  return <LoadedTable {...props} storedState={stored.data ?? {}} />;
}

function LoadedTable({
  caches,
  isFetching,
  refreshing,
  onRefresh,
  onTerminate,
  toolbar,
  storedState,
}: Props & { storedState: ColumnState }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const [search, setSearch] = useState("");
  const columns = useMemo(
    () => cacheColumns(t, ctx, onRefresh, refreshing, onTerminate),
    [t, ctx, onRefresh, refreshing, onTerminate]
  );
  const searched = useMemo(() => {
    const term = search.trim().toLowerCase();
    const sorted = [...caches].sort(
      (a, b) =>
        cacheStateRank(a.state) - cacheStateRank(b.state) ||
        a.status.name.localeCompare(b.status.name)
    );
    if (!term) return sorted;
    return sorted.filter((cache) =>
      [
        cache.status.qualifiedName,
        t(CACHE_STATE_KEYS[cache.state]),
        cache.status.lastRefreshError,
      ].some((text) => text?.toLowerCase().includes(term))
    );
  }, [caches, search, t]);
  const state = useTableState({ restoredState: storedState });
  const table = useDataTable<CacheEntry>({
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
        {toolbar && <div className="ml-auto">{toolbar}</div>}
      </div>
      <DataTable<CacheEntry>
        table={table}
        columns={[]}
        data={[]}
        isFetching={isFetching}
        showPagination={false}
        dense
        emptyState={t("nothingFound")}
      />
    </>
  );
}
