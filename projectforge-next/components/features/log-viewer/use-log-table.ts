"use client";

import { useMemo, useState } from "react";
import type { ColumnDef, SortingState } from "@tanstack/react-table";
import {
  useColumnStatePersistence,
  useDataTable,
  useStoredColumnState,
  useTableState,
  type ColumnState,
} from "@/components/data-table";

/**
 * The column state stored for a log table in the user's prefs (`AdminLogViewerRest.columnStates`), read
 * before the table is rendered, so it doesn't jump from the default layout to the stored one.
 * @param grid The path of the prefs below `/rs/` (`adminLogViewer`, `adminLogViewer/logFile`); none for a
 * table whose columns aren't stored (the user's log viewer).
 */
export function useStoredLogColumns(grid: string | undefined) {
  const stored = useStoredColumnState(grid);
  return {
    pending: grid !== undefined && stored.isPending,
    state: (stored.data ?? {}) as ColumnState,
  };
}

/** The layout of a log table, handed on to the tables of its contexts, so their columns line up with it. */
export type LogColumnLayout = Pick<
  ColumnState,
  "columnVisibility" | "columnPinning" | "columnSizing" | "columnOrder"
>;

/**
 * A log table whose columns can be resized, pinned, hidden and reordered (DataTableColumnPanel), stored in the
 * user's prefs as those of every other list (see CachesTable).
 */
export function useLogTable<E>({
  grid,
  restoredState,
  columns,
  data,
  getRowId,
  initialSorting,
}: {
  grid: string | undefined;
  restoredState: ColumnState;
  columns: ColumnDef<E, unknown>[];
  data: E[];
  getRowId: (row: E) => string;
  initialSorting?: SortingState;
}) {
  const state = useTableState({ restoredState, initialSorting });
  const table = useDataTable<E>({
    columns,
    data,
    sorting: state.sorting,
    onSortingChange: state.setSorting,
    columnVisibility: state.columnVisibility,
    onColumnVisibilityChange: state.setColumnVisibility,
    columnPinning: state.columnPinning,
    onColumnPinningChange: state.setColumnPinning,
    columnSizing: state.columnSizing,
    onColumnSizingChange: state.setColumnSizing,
    columnOrder: state.columnOrder,
    onColumnOrderChange: state.setColumnOrder,
    enableColumnFilters: false,
    enableColumnResizing: true,
    // All rows on one page: the queries are limited by the backend.
    manualPagination: true,
    getRowId,
  });
  useColumnStatePersistence(grid, {
    sorting: state.sorting,
    columnVisibility: state.columnVisibility,
    columnPinning: state.columnPinning,
    columnSizing: state.columnSizing,
    columnOrder: state.columnOrder,
  });
  const layout: LogColumnLayout = {
    columnVisibility: state.columnVisibility,
    columnPinning: state.columnPinning,
    columnSizing: state.columnSizing,
    columnOrder: state.columnOrder,
  };
  // Back to the columns as declared; the persistence then stores the empty state.
  const resetColumns = () => {
    state.setSorting(initialSorting ?? []);
    state.setColumnVisibility({});
    state.setColumnPinning({});
    state.setColumnSizing({});
    state.setColumnOrder([]);
  };
  return { table, layout, resetColumns };
}

/**
 * The table of a context: the columns of the table it belongs to in that table's layout ([LogColumnLayout],
 * not stored on its own), in the order of the file resp. memory.
 */
export function useContextTable<E>({
  layout,
  columns,
  data,
  getRowId,
}: {
  layout: LogColumnLayout;
  columns: ColumnDef<E, unknown>[];
  data: E[];
  getRowId: (row: E) => string;
}) {
  const noop = () => {};
  return useDataTable<E>({
    columns,
    data,
    columnVisibility: layout.columnVisibility ?? {},
    onColumnVisibilityChange: noop,
    columnPinning: layout.columnPinning ?? {},
    onColumnPinningChange: noop,
    columnSizing: layout.columnSizing ?? {},
    onColumnSizingChange: noop,
    columnOrder: layout.columnOrder ?? [],
    onColumnOrderChange: noop,
    enableColumnFilters: false,
    manualPagination: true,
    getRowId,
  });
}

/** Which rows of a log table have their context opened, see ContextToggle. */
export function useOpenContexts() {
  const [open, setOpen] = useState<ReadonlySet<string>>(new Set());
  // A new object only with a change, so the columns built from it stay as they are otherwise.
  return useMemo(
    () => ({
      isOpen: (id: string) => open.has(id),
      toggle: (id: string) => {
        const next = new Set(open);
        if (!next.delete(id)) next.add(id);
        setOpen(next);
      },
    }),
    [open]
  );
}
